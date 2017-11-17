package autodiscovery;

/**
 * Entry point for autodiscovery. Discoverers list contains ordered collection of discoverers.
 * As soon as one of them finds master ip the whole process cancels.
 * 
 * Chain of responsibilities is used. SlaveAutodiscoverer -> SlaveLocalAutodiscoverer -> SlaveGlobalAutodiscoverer
 */
public class SlaveAutodiscoverer implements Autodiscovery {

	// local autodiscoverer here
	private Autodiscovery autodiscovery;
	
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
