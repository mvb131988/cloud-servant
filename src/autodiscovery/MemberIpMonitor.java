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
		
		//dynamic configuration with ip addresses
		Path pathTxt = app.getPathSys().resolve(Paths.get("members.txt"));
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
		
		bro.persistMembersDescriptors(Paths.get("members.txt"),
								  	  this.memberId,
									  ds.stream()
										.map(d -> d.getMd())
										.collect(Collectors.toList()));
	}
	
	/**
	 * Walk through the list of members and check if exist any CLOUD member with undefined ip.
	 */ 
	public boolean existEmptyIp() {
		List<EnhancedMemberDescriptor> cloudDs = 
				ds.stream()
				  .filter(d -> MemberType.CLOUD.equals(d.getMd().getMemberType()) 
						 	   && null == d.getMd().getIpAddress())
				  .collect(Collectors.toList());
		return cloudDs.size() != 0;
	}
	
	public void updateIp(String ip) {
		
	}
	
	public List<String> getNotNullIps() {
		return null;
	}
}
