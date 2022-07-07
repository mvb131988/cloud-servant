package autodiscovery;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.InitializationException;
import exception.NotUniqueSourceMemberException;
import exception.WrongSourceMemberId;
import main.AppProperties;
import repository.BaseRepositoryOperations;

public class MemberIpMonitor {

	private Logger logger = LogManager.getRootLogger();

	private BaseRepositoryOperations bro;
	
	// local member id
	private String memberId;
	
	// list of all external members (their configuration including id, type, ip and failure 
	// counter) in the cluster  
	private List<EnhancedMemberDescriptor> ds;
	
	//dynamic configuration with ip addresses
	private Path pathTxt;
	
	public MemberIpMonitor() {}
	
	public MemberIpMonitor(BaseRepositoryOperations bro, AppProperties app) 
			throws InitializationException 
	{
		this.bro = bro;
		
		memberId = null;
		//member descriptors without ip addresses (static configuration)
		List<MemberDescriptor> ds0 = null;
		try {
			memberId = bro.loadLocalMemberId("members.properties");
			ds0 = bro.loadRemoteMembers("members.properties");
		} catch (IOException ex) {
			logger.error("Exception during members property (static file) file read", ex);
			throw new InitializationException("Exception during members property (static file) "
					+ "file read" , ex);
		}
		
		pathTxt = app.getPathSys().resolve(Paths.get("members.txt"));
		try {
			bro.createMembersFileIfNotExist(pathTxt, memberId, ds0);
		} catch (IOException ex) {
			logger.error("Exception during members configuration file creation", ex);
			throw new InitializationException("Exception during members configuration "
					+ "file creation", ex);
		}
		
		//load members.txt into internal structure
		try {
			ds = bro.loadRemoteMembers(pathTxt).stream()
											   .map(md -> new EnhancedMemberDescriptor(md, 0))
											   .collect(Collectors.toList());
		} catch (IOException ex) {
			logger.error("Exception during members txt (dynamic file) file read", ex);
			throw new InitializationException("Exception during members txt (dynamic file) "
					+ "file read", ex);
		}
	}
	
	/**
	 * Walk through the list of members and check if exist member of type SOURCE with an ip
	 * already set and failure counter not reached the allowed limit. Note here could be more 
	 * than one member of type SOURCE, however it is a requirement that only one member of type 
	 * SOURCE with defined ip could exist.   
	 * 
	 * @return true if exists only one SOURCE member with defined ip
	 * @throws NotUniqueSourceMemberException 
	 */
	public boolean isActiveSourceMember() throws NotUniqueSourceMemberException {
		//not reachable(makes sense no check ip not null)
		Predicate<EnhancedMemberDescriptor> notNull = d -> d.getMd().getIpAddress() != null;
		Predicate<EnhancedMemberDescriptor> isActive = d -> d.getFailureCounter() == 0;
		
		Map<MemberType, List<EnhancedMemberDescriptor>> dsByType = 
				ds.stream()
				  .filter(notNull.and(isActive))
				  .collect(Collectors.groupingBy(d -> d.getMd().getMemberType()));
		
		List<EnhancedMemberDescriptor> dsSource = dsByType.get(MemberType.SOURCE);
		if(dsSource != null && dsSource.size() > 1) {
			throw new NotUniqueSourceMemberException();
		}
		
		if(null == dsSource || dsSource.size() == 0) {
			return false;
		}
		
		return dsSource.size() == 1;
	}
	
	/**
	 * Find failure counter for the given SOURCE member. Only one SOURCE member could exist at a
	 * time. If no SOURCE member exist, return 0; 
	 * 
	 * @return failure counter value or 0 if members exist
	 * @throws NotUniqueSourceMemberException
	 */
	public int sourceFailureCounter() throws NotUniqueSourceMemberException {
		//not reachable(makes sense no check ip not null)
		Map<MemberType, List<EnhancedMemberDescriptor>> dsByType = 
				ds.stream()
				  .filter(d -> null != d.getMd().getIpAddress())
				  .collect(Collectors.groupingBy(d -> d.getMd().getMemberType()));
		
		List<EnhancedMemberDescriptor> dsSource = dsByType.get(MemberType.SOURCE);
		if(dsSource != null && dsSource.size() > 1) {
			throw new NotUniqueSourceMemberException();
		}
		
		return null != dsSource ? dsSource.get(0).getFailureCounter() : 0;
	}
	
	//TODO: synchronized is required
	/**
	 * Set source ip address for the given member id. If there is already one overrides it.
	 * Reset failure counter (to 0). 
	 * 
	 * @param memberId
	 * @param sourceIp
	 * @throws NotUniqueSourceMemberException
	 * @throws WrongSourceMemberId
	 * @throws IOException 
	 */
	public void setSourceIp(String memberId, String sourceIp) 
			throws NotUniqueSourceMemberException, WrongSourceMemberId, IOException
	{
		Predicate<EnhancedMemberDescriptor> equals = d -> d.getMd().getMemberId().equals(memberId);
		Predicate<EnhancedMemberDescriptor> isSource = 
				d -> MemberType.SOURCE.equals(d.getMd().getMemberType());
		
		List<EnhancedMemberDescriptor> dsSource = ds.stream()
			  		 								.filter(isSource.and(equals))
			  		 								.collect(Collectors.toList());
		
		if(dsSource.size() == 0) {
			throw new WrongSourceMemberId();
		}
		
		dsSource.get(0).getMd().setIpAddress(sourceIp);
		dsSource.get(0).setFailureCounter(0);
		
		bro.persistMembersDescriptors(pathTxt,
								  	  this.memberId,
									  ds.stream()
										.map(d -> d.getMd())
										.collect(Collectors.toList()));
	}
	
	/**
	 * Walk through the list of members and check if exist any CLOUD member with undefined ip
	 * or any CLOUD member's failure counter exceeded the limit.
	 * 
	 * @return true if all CLOUD members are active (ip address not null and failure counter limit 
	 * 		   not reached) and false otherwise
	 */ 
	public boolean areActiveCloudMembers() {
		Predicate<EnhancedMemberDescriptor> isCloud =
				d -> MemberType.CLOUD.equals(d.getMd().getMemberType());
		Predicate<EnhancedMemberDescriptor> notNull = d -> d.getMd().getIpAddress() != null;
		Predicate<EnhancedMemberDescriptor> isActive = d -> d.getFailureCounter() == 0;
		
		List<EnhancedMemberDescriptor> cloudDs = 
				ds.stream()
				  .filter(isCloud.and(notNull).and(isActive))
				  .collect(Collectors.toList());
		
		// CLOUD members counter
		long counter = ds.stream().filter(isCloud).count();
		
		return cloudDs.size() == counter;
	}
	
	/**
	 * Update members ip addresses with ip addresses that come from mds. 
	 * Not all members might be present in mds (some member might be down during ip scan). 
	 * 
	 * @param mds - list of newly discovered members
	 */
	public void setCloudIps(List<MemberDescriptor> mds) throws IOException {
		Map<String, MemberDescriptor> map = 
				mds.stream().collect(Collectors.toMap(d -> d.getMemberId(), d -> d));
		
		for(int i=0; i<ds.size(); i++) {
			MemberDescriptor d = map.get(ds.get(i).getMd().getMemberId());
			if(d != null) {
				ds.get(i).getMd().setIpAddress(d.getIpAddress());
				ds.get(i).setFailureCounter(0);
			}
		}
		
		bro.persistMembersDescriptors(pathTxt,
							  	  	  this.memberId,
									  ds.stream()
										.map(d -> d.getMd())
										.collect(Collectors.toList()));
	}
	
	/**
	 * Find max failure counter among all CLOUD members.
	 * 
	 * @return max failure counter among all CLOUD members
	 */
	public int cloudFailureCounter() {
		// max failure counter across all CLOUD nodes
		int counter= 
				ds.stream()
				  .filter(d -> MemberType.CLOUD.equals(d.getMd().getMemberType()))
		          .map(d -> d.getFailureCounter())
		          .max(Integer::compare)
		          .get();
		
		return counter;
	}
	
	public List<String> getNotNullIps() {
		return null;
	}
}
