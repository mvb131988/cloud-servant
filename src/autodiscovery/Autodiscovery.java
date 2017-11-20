package autodiscovery;

/**
 *	Autodiscovery process scans range of ip addresses in order to find a machine where
 *  master communication process runs. Is supposed to be used on slave communicayion process
 *  side. 
 */
public interface Autodiscovery {
	
	/**
	 * Initiates autodiscovery process and returns found master ip address.
	 * 
	 * @param failureCounter - external(relative to autodiscovery package) parameter. Shows how many times
	 * 						   slave failed to establish connection with master
	 * 
	 * @return master ip address or null if not found
	 */
	public String discover(int failureCounter);
	
}
