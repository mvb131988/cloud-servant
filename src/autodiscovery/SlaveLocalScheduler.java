package autodiscovery;

import java.time.ZonedDateTime;

/**
 * To cases of scheduling exist:
 * 
 * (1) Firstly master-slave connection exist but after some time it fails. Schedule next scan relative to the moment of
 * 	   connection failure(checkAndUpdateBaseTime method)
 * (2) Autodiscovery process terminates without any ip founded. To avoid immediate relaunch move launch date to some
 * 	   period later(updateBaseTime method)
 * 
 */
public class SlaveLocalScheduler implements SlaveScheduler {

	private ZonedDateTime baseTime;
	
	//TODO: move to constants
	private int scanPeriodInSeconds = 60;
	
	@Override
	public boolean isScheduled(int failureCounter, String masterIp) {
		boolean isScheduled = false;
		
		// First scan scheduling on startup only
		if(failureCounter == 0 && masterIp == null) {
			isScheduled = true;
		}
		
		// baseTime must be not null
		if(failureCounter > 1 && ZonedDateTime.now().toInstant().isAfter(baseTime.plusSeconds(scanPeriodInSeconds).toInstant())) {
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
			baseTime = ZonedDateTime.now().plusSeconds(scanPeriodInSeconds);
			return true;
		}
		return false;
	}

	/**
	 * Update base time after autodetection process is terminated.
	 */
	@Override
	public void updateBaseTime() {
		baseTime = ZonedDateTime.now().plusSeconds(scanPeriodInSeconds);
	}
	
}
