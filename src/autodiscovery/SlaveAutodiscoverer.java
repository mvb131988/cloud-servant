package autodiscovery;

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
	
	// contains last found master ip or null otherwise
	private String masterIp;
	
	public SlaveAutodiscoverer(Autodiscovery autodiscovery, AppProperties ap) {
		super();
		this.autodiscovery = autodiscovery;
		this.bigTimeout = ap.getBigPoolingTimeout();
	}
	
	@Override
	public String discover(int failureCounter) {
		String newMasterIp = null;
		
		if(masterIp == null) {
			while(newMasterIp == null) {
				newMasterIp = autodiscovery.discover(failureCounter);
				
				try {
					Thread.sleep(bigTimeout);
				} catch (InterruptedException e) {
					logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e); 
				}
				
			}
			masterIp = newMasterIp;
		} 
		else {
			newMasterIp = autodiscovery.discover(failureCounter);

			//if new master ip not found, use old value. If its value not null then it's the last found master ip.
			//try it to reconnect to the master.
			if(newMasterIp != null) {
				masterIp = newMasterIp;
			}
		}
		
		return masterIp;
	}

}
