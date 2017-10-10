package provider;

/**
 * Defines a rule(point in a time) when slave communication thread requested 
 * transfer.
 * Interval has to be set in minutes.  
 */
public class SlaveTransferScheduler extends AbstractScheduler {

	public SlaveTransferScheduler() {
		minutesInterval = 5;
	}
	
}
