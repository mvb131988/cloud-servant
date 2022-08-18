package autodiscovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpScannerResult;
import main.AppProperties;

/**
 * Intended for SOURCE member autodiscovering when SOURCE member and CLOUD member run in the
 * same local network. SOURCE member is going to be discovered by CLOUD member 
 */
public class SourceMemberAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private MemberAutodiscoveryScheduler scheduler;
	
	private String localRanges;
	
	private MemberDescriptor md;
	
	private MemberIpMonitor mim;
	
	private String memberId;
	
	public SourceMemberAutodiscoverer(MemberAutodiscoveryScheduler scheduler, 
									  IpFJPScanner ipScanner,
									  MemberIpMonitor mim,
									  AppProperties ap) 
	{
		this.scheduler = scheduler;
		this.ipScanner = ipScanner;
		this.mim = mim; 
		this.localRanges = ap.getLocalRanges();
		this.memberId = ap.getMemberId();
	}
	
	//TODO: When CLOUD member is running it could connect to itself during autodiscovery process
	//      ignore this result
	
	//TODO: no return type
	//		change failureCounter to requestScan
	//		when requestScan set check if scan timeout is reached and initiate new scan
	@Override
	public List<String> discover(int failureCounter) {
		this.md = null;
		List<IpScannerResult> ips = new ArrayList<IpScannerResult>();
		
		// Local autodiscovery
		scheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isLocalScheduled = scheduler.isScheduled(failureCounter);
		
		if(isLocalScheduled) {
			logger.info("[" + this.getClass().getSimpleName() + "] local scan start");

			// sourceIp must have only one element as in local network only one source 
			// member is allowed
			ips.addAll(ipScanner.scan(localRanges));
			
			List<MemberDescriptor> mds0 = new ArrayList<MemberDescriptor>();
			// at most one SOURCE member is allowed in local autodiscovery scan 
			for (IpScannerResult ip: ips) {
				String memberId = ip.getMemberId();
				MemberType memberType = mim.memberTypeByMemberId(memberId); 
				MemberDescriptor md0 = new MemberDescriptor(memberId, memberType, ip.getIp());
				if (!this.memberId.equals(md0.getMemberId())) {
					mds0.add(md0);
				}
			}
			
			if(mds0.size() > 1) {
				//throw only one CLOUD member is allowed in local network (datacenter)
			}
			
			if(mds0.size() == 1) {
				md = mds0.get(0);
			}
			
			mds0.stream().forEach(
			    md -> logger.info("[" + this.getClass().getSimpleName() + "] "
			                + "local scan finish with source ip = " + md.getIpAddress())
			);
			
			scheduler.updateBaseTime();
		} 
		
		return null;
	}

	public MemberDescriptor getMemberDescriptor() {
		return md;
	}

}
