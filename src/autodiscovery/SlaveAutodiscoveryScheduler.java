package autodiscovery;

import java.time.ZonedDateTime;

import main.AppProperties;

/**
 * To cases of scheduling exist:
 * 
 * (1) Firstly master-slave connection exist but after some time it fails. Schedule next scan relative to the moment of
 * 	   connection failure(checkAndUpdateBaseTime method)
 * (2) Autodiscovery process terminates without any ip founded. To avoid immediate relaunch move launch date to some
 * 	   period later(updateBaseTime method)
 * 
 */
public class SlaveAutodiscoveryScheduler implements SlaveScheduler {

	private ZonedDateTime baseTime;
	
	// local autodetection period in seconds
	private int autodetectionPeriod;
	
	public SlaveAutodiscoveryScheduler(int autodetectionPeriod) {
		this.autodetectionPeriod = autodetectionPeriod;
	}
	
	@Override
	public boolean isScheduled(int failureCounter) {
		boolean isScheduled = false;
		
		// First scan scheduling on startup only
		if(failureCounter == 0) {
			isScheduled = true;
		}
		
		// baseTime must not be null
		if(failureCounter > 1 && ZonedDateTime.now().toInstant().isAfter(baseTime.plusSeconds(autodetectionPeriod).toInstant())) {
			isScheduled = true;
		}
		
		return isScheduled;
	}
	
	/**
	 * Base time shift to a later date on first master - slave communication failure.
	 * On first master - slave communication failure postpone autodetection process.
	 * This is because we've just had a valid ip, hence we can use it to reconnect to the master(during some interval).
	 */
	@Override
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
	@Override
	public void updateBaseTime() {
		baseTime = ZonedDateTime.now().plusSeconds(autodetectionPeriod);
	}
	
}
