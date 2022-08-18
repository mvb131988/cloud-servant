package autodiscovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpScannerResult;
import main.AppProperties;

/**
 * Intended for CLOUD member autodiscovering when all CLOUD members are assigned global ip
 * address. All CLOUD members are going to be discovered by all CLOUD members. 
 */
public class CloudMemberAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private MemberAutodiscoveryScheduler scheduler;
	
	private MemberIpMonitor mim;

	private String globalRanges;
	
	private List<MemberDescriptor> mds;
	
	private String memberId;
	
	public CloudMemberAutodiscoverer(MemberAutodiscoveryScheduler scheduler, 
									 IpFJPScanner ipScanner,
									 MemberIpMonitor mim,
									 AppProperties ap) {
		this.scheduler = scheduler;
		this.ipScanner = ipScanner;
		this.mim = mim;
		this.globalRanges = ap.getGlobalRanges();
		this.memberId = ap.getMemberId();
		this.mds = new ArrayList<>();
	}
	
	//TODO: no return type
	//		change failureCounter to requestScan
	//		when requestScan set check if scan timeout is reached and initiate new scan
	@Override
	public List<String> discover(int failureCounter) {
		this.mds = new ArrayList<>();
		List<IpScannerResult> cloudIps = new ArrayList<IpScannerResult>();
		
		// Global autodiscovery
		scheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isScheduled = scheduler.isScheduled(failureCounter);
		
		if(isScheduled) {
			logger.info("[" + this.getClass().getSimpleName() + "] global scan start");
			
			cloudIps = ipScanner.scan(globalRanges);
			
			logger.info("Number of found ips: " + cloudIps.size());
			
			for(IpScannerResult cloudIp: cloudIps) {
				String memberId = cloudIp.getMemberId();
				MemberType memberType = mim.memberTypeByMemberId(memberId);
				
				MemberDescriptor md0 = new MemberDescriptor(memberId, memberType, cloudIp.getIp());
				if (!this.memberId.equals(md0.getMemberId())) {
					mds.add(md0);
				}
			}
			
			mds.stream().forEach(
				md -> logger.info("Global scan finished with member : " + md)
			);
						
			scheduler.updateBaseTime();
		} 
		
		return null;
	}

	public List<MemberDescriptor> getMds() {
		return mds;
	}

}
