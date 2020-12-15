package autodiscovery;

import java.util.List;

/**
 *	Autodiscovery process scans range of ip addresses in order to find a machine where
 *  master communication process runs. Is supposed to be used on slave communicayion process
 *  side. 
 */
public interface Autodiscovery {
	
	/**
	 * Initiates autodiscovery process and returns found all candidates for master ip address.
	 * 
	 * @param failureCounter - external(relative to autodiscovery package) parameter. Shows how many times
	 * 						             slave has failed to establish connection with master
	 * 
	 * @return all candidates for master ip address or empty list if not found
	 */
	public List<String> discover(int failureCounter);
	
}
