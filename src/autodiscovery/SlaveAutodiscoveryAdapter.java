package autodiscovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;

/**
 * Has to be used in single thread only. Sequence of public methods must be failure() -> getMasterIp() 
 */
public class SlaveAutodiscoveryAdapter {

	private Logger logger = LogManager.getRootLogger();
	
	private AppProperties ap;
	
	public SlaveAutodiscoveryAdapter(AppProperties ap) {
		this.ap = ap;
	}
	
	/**
	 * Similar with failure, but invoked once at startup;
	 */
	public void startup() {
		failure();
	}
	
	/**
	 * Signals that master-slave communication is broken.
	 * Detects and saves failure time. Decides if autodiscovery process has to be initiated or not.
	 * If autodiscovery process is initiated the method blocks until the process completes. 
	 */
	public void failure() {
		
	}
	
	/**
	 * @return master ip address
	 */
	public String getMasterIp() {
		return ap.getMasterIp();
	}
	
}
