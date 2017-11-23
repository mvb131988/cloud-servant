package autodiscovery;

import ipscanner.IpScanner;
import main.AppProperties;

/**
 * Intended for master autodiscovering when master and slave run in the same local network 
 */
public class SlaveLocalAutodiscoverer implements Autodiscovery {

	private IpScanner ipScanner; 
	
	private AppProperties ap;
	
	// global autodiscoverer here
	private Autodiscovery autodiscovery;
	
	private SlaveLocalScheduler slaveScheduler;
	
	private String localRanges;
	
	public SlaveLocalAutodiscoverer(Autodiscovery autodiscovery, SlaveLocalScheduler slaveScheduler, IpScanner ipScanner, AppProperties ap) {
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
		boolean isLocalScheduled = slaveScheduler.isScheduled(failureCounter, masterIp);
		if(isLocalScheduled) {
			masterIp = discoverInternally();
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
		if(ip == null) {
			ip = ap.getMasterIp();
		}
		
		return ip;
	}

}
