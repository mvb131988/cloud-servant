package autodiscovery;

import java.time.ZonedDateTime;

/**
 * Is used on any CLOUD member (any CLOUD member is going to discover all other CLOUD members) to
 * schedule autodiscovery process.
 * 
 */
//TODO: get rid of failureCounter, use schedule flag instead
public class MemberAutodiscoveryScheduler {

	private ZonedDateTime baseTime;
	
	// local autodetection period in seconds
	private int autodetectionPeriod;
	
	public MemberAutodiscoveryScheduler(int autodetectionPeriod) {
		this.autodetectionPeriod = autodetectionPeriod;
	}
	
	public boolean isScheduled(int failureCounter) {
		boolean isScheduled = false;
		
		// First scan scheduling on startup only
		if(failureCounter == 0) {
			isScheduled = true;
		}
		
		// baseTime must not be null
		if(failureCounter > 1 && 
				ZonedDateTime.now().toInstant().isAfter(
						baseTime.plusSeconds(autodetectionPeriod).toInstant())) {
			isScheduled = true;
		}
		
		return isScheduled;
	}
	
	/**
	 * Base time shift to a later date on first master - slave communication failure.
	 * On first master - slave communication failure postpone autodetection process.
	 * This is because we've just had a valid ip, hence we can use it to reconnect to the master
	 * (during some interval).
	 */
	@Deprecated
	public boolean checkAndUpdateBaseTime(int failureCounter) {
		if(failureCounter == 1) {
			baseTime = ZonedDateTime.now().plusSeconds(autodetectionPeriod);
			return true;
		}
		return false;
	}

	/**
	 * Update base time after autodetection process is terminated.
	 */
	public void updateBaseTime() {
		baseTime = ZonedDateTime.now().plusSeconds(autodetectionPeriod);
	}
	
}
