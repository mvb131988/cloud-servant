package autodiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ipscanner.IpFJPScanner;
import main.AppProperties;

public class SlaveGlobalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private String globalRanges;
	
	private List<MemberDescriptor> mds;
	
	public SlaveGlobalAutodiscoverer(SlaveAutodiscoveryScheduler slaveScheduler, 
									 IpFJPScanner ipScanner,
									 AppProperties ap) {
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.globalRanges = ap.getGlobalRanges();
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
			
			cloudIps.stream().forEach(
				ip -> logger.info("[" + this.getClass().getSimpleName() 
						+ "] global scan finished with cloud ip = " + ip)
			);
						
			slaveScheduler.updateBaseTime();
		} 
		
		mds = cloudIps.stream()
					  .map(ip -> new MemberDescriptor(null, MemberType.CLOUD, ip))
					  .collect(Collectors.toList());
		
		return null;
	}

	public List<MemberDescriptor> getMds() {
		return mds;
	}

}
