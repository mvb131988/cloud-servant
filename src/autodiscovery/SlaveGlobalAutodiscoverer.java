package autodiscovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ipscanner.IpFJPScanner;
import main.AppProperties;
import repository.SysManager;

public class SlaveGlobalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private String globalRanges;
	
	private SysManager sysManager;
	
	public SlaveGlobalAutodiscoverer(SlaveAutodiscoveryScheduler slaveScheduler, 
									 IpFJPScanner ipScanner, 
									 SysManager sysManager,
									 AppProperties ap) {
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.sysManager = sysManager;
		this.globalRanges = ap.getGlobalRanges();
	}
	
	@Override
	public String discover(int failureCounter) {
		String masterIp = sysManager.getMasterIp();
		
		// Global autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isScheduled = slaveScheduler.isScheduled(failureCounter);
		if(isScheduled) {
			
			logger.info("[" + this.getClass().getSimpleName() + "] global scan start");
			masterIp = ipScanner.scan(globalRanges);
			if(masterIp != null) {
				sysManager.persistMasterIp(masterIp);
			}
			logger.info("[" + this.getClass().getSimpleName() + "] global scan finish with masterIp = " + masterIp);
			
			slaveScheduler.updateBaseTime();
		} 
		
		return masterIp;
	}

}
