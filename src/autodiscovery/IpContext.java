package autodiscovery;

public class IpContext {

	private boolean allIpsFound;
	private int failureCounter;
	
	public IpContext(boolean allIpsFound, int failureCounter) {
		super();
		this.allIpsFound = allIpsFound;
		this.failureCounter = failureCounter;
	}

	public boolean areAllIpsFound() {
		return allIpsFound;
	}
	
	public int getFailureCounter() {
		return failureCounter;
	}
	
}
