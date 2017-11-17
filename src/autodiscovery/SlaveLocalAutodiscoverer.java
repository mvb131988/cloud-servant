package autodiscovery;

import ipscanner.IpScanner;
import main.AppProperties;

public class SlaveLocalAutodiscoverer implements Autodiscovery {

	private IpScanner ipScanner; 
	
	private AppProperties ap;
	
	// global autodiscoverer here
	private Autodiscovery autodiscovery;
	
	private SlaveLocalScheduler slaveScheduler;
	
	public SlaveLocalAutodiscoverer(Autodiscovery autodiscovery, SlaveLocalScheduler slaveScheduler, IpScanner ipScanner, AppProperties ap) {
		this.autodiscovery = autodiscovery;
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.ap = ap;
	}
	
	@Override
	public String discover(int failureCounter) {
		String masterIp = null;
		
		// Local autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isLocalScheduled = slaveScheduler.isScheduled(failureCounter, masterIp);
		if(isLocalScheduled) {
			masterIp = discoverInternally();
		} 
		
		// Global autodiscovery
		if((isLocalScheduled && masterIp == null) || !isLocalScheduled) {
			//is global scan scheduled
			//invoke global autodiscoverer
			masterIp = autodiscovery.discover(failureCounter);
		}
		
		return masterIp;
	}
	
	private String discoverInternally() {
		//TODO: Testing implementation
		String ip = ipScanner.scan();
		if(ip == null) {
			ip = ap.getMasterIp();
		}
		return ip;
	}

}
