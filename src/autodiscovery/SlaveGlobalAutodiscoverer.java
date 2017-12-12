package autodiscovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ipscanner.IpFJPScanner;
import main.AppProperties;

public class SlaveGlobalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private String globalRanges;
	
	public SlaveGlobalAutodiscoverer(SlaveAutodiscoveryScheduler slaveScheduler, IpFJPScanner ipScanner, AppProperties ap) {
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.globalRanges = ap.getGlobalRanges();
	}
	
	@Override
	public String discover(int failureCounter) {
		String masterIp = null;
		
		// Global autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isScheduled = slaveScheduler.isScheduled(failureCounter);
		if(isScheduled) {
			
			logger.info("[" + this.getClass().getSimpleName() + "] global scan start");
			masterIp = ipScanner.scan(globalRanges);
			logger.info("[" + this.getClass().getSimpleName() + "] global scan finish with masterIp = " + masterIp);
			
			slaveScheduler.updateBaseTime();
		} 
		
		return null;
	}

}
