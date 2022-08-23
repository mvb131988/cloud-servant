package autodiscovery;

/**
 *	Autodiscovery process scans range of ip addresses in order to find all machines where
 *  CLOUD and SOURCE member processes are running. It is supposed to be used on any CLOUD member 
 *  in order to find all other CLOUD and SOURCE members that exist in the system. 
 */
public interface Autodiscovery {
	
	/**
	 * Initiates autodiscovery process.
	 * 
	 * @param ipContext - context with parameters that are required to schedule autodiscovery 
	 * 					  process
	 */
	public void discover(IpContext ipContext);
	
}
