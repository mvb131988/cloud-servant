package autodiscovery;

import java.util.ArrayList;
import java.util.List;

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
	
	// global autodiscoverer here
	private Autodiscovery autodiscovery;
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private String localRanges;
	
	private MemberDescriptor md;
	
	public SlaveLocalAutodiscoverer(Autodiscovery autodiscovery, SlaveAutodiscoveryScheduler slaveScheduler, IpFJPScanner ipScanner, AppProperties ap) {
		this.autodiscovery = autodiscovery;
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.localRanges = ap.getLocalRanges();
	}
	
	//TODO: change return type to void
	@Override
	public List<String> discover(int failureCounter) {
		List<String> masterIps = new ArrayList<String>();
		md = null;
		
		// Local autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isLocalScheduled = slaveScheduler.isScheduled(failureCounter);
		if(isLocalScheduled) {
			logger.info("[" + this.getClass().getSimpleName() + "] local scan start");

			//no filtering, all candidates in local network are supposed to be valid cloud-servant nodes
			masterIps.addAll(ipScanner.scan(localRanges));
			
			masterIps.stream().forEach(
			    masterIp -> logger.info("[" + this.getClass().getSimpleName() + "] "
			                + "local scan finish with masterIp = " + masterIp)
			);
			
			slaveScheduler.updateBaseTime();
		} 
		
		// Global autodiscovery
		if(masterIps.size() == 0 || !isLocalScheduled) {
			//if global scan scheduled, invoke global auto discoverer
		  //no filtering, filtering is done at the level of global auto discovery scan
		  masterIps.addAll(autodiscovery.discover(failureCounter));
		}
		
		if(masterIps.size() == 1) {
			md = new MemberDescriptor(masterIps.get(0), MemberType.SOURCE, null);
		}
			
		return masterIps;
	}

	public MemberDescriptor getMemberDescriptor() {
		return md;
	}

}
