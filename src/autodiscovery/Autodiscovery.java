package autodiscovery;

public interface Autodiscovery {
	
	/**
	 * @param failureCounter - external(relative to autodiscovery package) parameter. Shows how many times
	 * 						   slave failed to establish connection with master
	 * 
	 * @return master ip address or null if not found
	 */
	public String discover(int failureCounter);
	
}
