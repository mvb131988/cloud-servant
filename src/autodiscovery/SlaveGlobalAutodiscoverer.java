package autodiscovery;

import main.AppProperties;

public class SlaveGlobalAutodiscoverer implements Autodiscovery {

	private AppProperties ap;
	
	public SlaveGlobalAutodiscoverer(AppProperties ap) {
		this.ap = ap;
	}
	
	@Override
	public String discover(int failureCounter) {
		return null;
	}

}
