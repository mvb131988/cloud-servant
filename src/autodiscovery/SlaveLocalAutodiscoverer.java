package autodiscovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ipscanner.IpFJPScanner;
import main.AppProperties;

/**
 * Intended for master autodiscovering when master and slave run in the same local network 
 */
public class SlaveLocalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private AppProperties ap;
	
	// global autodiscoverer here
	private Autodiscovery autodiscovery;
	
	private SlaveLocalScheduler slaveScheduler;
	
	private String localRanges;
	
	public SlaveLocalAutodiscoverer(Autodiscovery autodiscovery, SlaveLocalScheduler slaveScheduler, IpFJPScanner ipScanner, AppProperties ap) {
		this.autodiscovery = autodiscovery;
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.ap = ap;
		this.localRanges = ap.getLocalRanges();
	}
	
	@Override
	public String discover(int failureCounter) {
		String masterIp = null;
		
		// Local autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isLocalScheduled = slaveScheduler.isScheduled(failureCounter);
		if(isLocalScheduled) {
			
			logger.info("[" + this.getClass().getSimpleName() + "] local scan start");
			masterIp = discoverInternally();
			logger.info("[" + this.getClass().getSimpleName() + "] local scan finish with masterIp = " + masterIp);
			
			slaveScheduler.updateBaseTime();
		} 
		
		// Global autodiscovery
		if(masterIp == null || !isLocalScheduled) {
			//is global scan scheduled
			//invoke global autodiscoverer
			masterIp = autodiscovery.discover(failureCounter);
		}
		
		return masterIp;
	}
	
	private String discoverInternally() {
		String ip = ipScanner.scan(localRanges);

		//TODO: Testing implementation
		//Redundant remove after test is completed
		if(ip == null) {
			ip = ap.getMasterIp();
		}
		
		return ip;
	}

}
