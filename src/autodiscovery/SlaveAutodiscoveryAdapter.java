package autodiscovery;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;

/**
 * Designed to be used in a single thread only.
 */
@Deprecated
public class SlaveAutodiscoveryAdapter {

	private Logger logger = LogManager.getRootLogger();
	
	//Prototype scope.
	private SlaveAutodiscoverer sa;
	
	private AppProperties ap;
	
	public SlaveAutodiscoveryAdapter(SlaveAutodiscoverer sa, AppProperties ap) {
		this.ap = ap;
		this.sa = sa;
	}
	
	/**
	 * Similar with failure, but invoked once at startup;
	 * 
	 * @return master ip address
	 */
	public List<String> startup(int failureCounter) {
		return failure(failureCounter);
	}
	
	/**
	 * Signals that master-slave communication is broken.
	 * Detects and saves failure time. Decides if autodiscovery process has to be initiated or not.
	 * If autodiscovery process is initiated the method blocks until the process completes. 
	 * 
	 * @return master ip address
	 */
	public List<String> failure(int failureCounter) {
		return sa.discover(failureCounter);
	}
	
}
