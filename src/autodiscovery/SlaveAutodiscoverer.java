package autodiscovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;

/**
 * Entry point for autodiscovery.
 *
 * Chain of responsibilities is used. SlaveAutodiscoverer -> SlaveLocalAutodiscoverer -> SlaveGlobalAutodiscoverer
 */
public class SlaveAutodiscoverer implements Autodiscovery {

	private Logger logger = LogManager.getRootLogger();
	
	private final int bigTimeout;
	
	// local autodiscoverer here
	private Autodiscovery autodiscovery;
	
	// contains all last found master ips or empty list otherwise
	private List<String> masterIps;
	
	public SlaveAutodiscoverer(Autodiscovery autodiscovery, AppProperties ap) {
		super();
		this.autodiscovery = autodiscovery;
		this.bigTimeout = ap.getBigPoolingTimeout();
		this.masterIps = new ArrayList<String>();
	}
	
	@Override
	public List<String> discover(int failureCounter) {
		List<String> newMasterIps = new ArrayList<>();
		
		if(masterIps.size() == 0) {
			int localFailureCounter = failureCounter;
			while(newMasterIps.size() == 0) {
			  newMasterIps.addAll(autodiscovery.discover(localFailureCounter));
				
				localFailureCounter++;
				
				try {
					Thread.sleep(bigTimeout);
				} catch (InterruptedException e) {
					logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e); 
				}
				
			}
			masterIps.addAll(newMasterIps);
		} 
		else {
		  newMasterIps.addAll(autodiscovery.discover(failureCounter));

			//if new master ip not found, use old value. If its value presents in the list then it's the last found master ip.
			//try it to reconnect to the master.
			if(newMasterIps.size() > 0) {
			  masterIps.clear();
				masterIps.addAll(newMasterIps);
			}
		}
		
		return masterIps;
	}

}
