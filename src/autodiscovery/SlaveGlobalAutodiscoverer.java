package autodiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpValidator;
import autodiscovery.ipscanner.IpValidatorResult;
import main.AppProperties;

public class SlaveGlobalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private IpValidator ipValidator;
	
	private MemberIpMonitor mim;

	private String globalRanges;
	
	private List<MemberDescriptor> mds;
	
	public SlaveGlobalAutodiscoverer(SlaveAutodiscoveryScheduler slaveScheduler, 
									 IpFJPScanner ipScanner,
									 IpValidator ipValidator,
									 MemberIpMonitor mim,
									 AppProperties ap) {
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.ipValidator = ipValidator;
		this.mim = mim;
		this.globalRanges = ap.getGlobalRanges();
		this.mds = new ArrayList<>();
	}
	
	//TODO: no return type
	//		change failureCounter to requestScan
	//		when requestScan set check if scan timeout is reached and initiate new scan
	@Override
	public List<String> discover(int failureCounter) {
		List<String> cloudIps = new ArrayList<String>();
		
		// Global autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isScheduled = slaveScheduler.isScheduled(failureCounter);
		
		if(isScheduled) {
			logger.info("[" + this.getClass().getSimpleName() + "] global scan start");
			
			cloudIps = ipScanner.scan(globalRanges);
			
			for(String cloudIp: cloudIps) {
				IpValidatorResult result = ipValidator.isValid(cloudIp);
				String memberId = result.isResult() ? result.getMemberId() : null;
				MemberType memberType = mim.memberTypeByMemberId(memberId);
				mds.add(new MemberDescriptor(memberId, memberType, cloudIp));
			}
			
			cloudIps.stream().forEach(
				ip -> logger.info("[" + this.getClass().getSimpleName() 
						+ "] global scan finished with cloud ip = " + ip)
			);
						
			slaveScheduler.updateBaseTime();
		} 
		
		return null;
	}

	public List<MemberDescriptor> getMds() {
		return mds;
	}

}
