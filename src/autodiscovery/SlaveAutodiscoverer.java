package autodiscovery;

/**
 * Entry point for autodiscovery.
 *
 * Chain of responsibilities is used. SlaveAutodiscoverer -> SlaveLocalAutodiscoverer -> SlaveGlobalAutodiscoverer
 */
public class SlaveAutodiscoverer implements Autodiscovery {

	// local autodiscoverer here
	private Autodiscovery autodiscovery;
	
	// contains last found master ip or null otherwise
	private String masterIp;
	
	public SlaveAutodiscoverer(Autodiscovery autodiscovery) {
		super();
		this.autodiscovery = autodiscovery;
	}
	
	@Override
	public String discover(int failureCounter) {
		String newMasterIp = autodiscovery.discover(failureCounter);
		
		if(newMasterIp != null) {
			masterIp = newMasterIp;
		}
		
		return masterIp;
	}

}
