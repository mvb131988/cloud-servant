package autodiscovery;

import java.time.ZonedDateTime;

public class SlaveLocalScheduler implements SlaveScheduler {

	private ZonedDateTime baseTime;
	
	//TODO: move to constants
	private int scanPeriodInSeconds = 60;
	
	@Override
	public boolean isScheduled(int failureCounter, String masterIp) {
		boolean isScheduled = false;
		
		// case1:
		// if counter = 0 scan immediately
		// baseTime indicates time of the last scan
		if(failureCounter == 0 && masterIp == null) {
			isScheduled = true;
		}
		
		// case2: 
		// failureCounter == 1
		
		// case3:
		// if counter > 1 check base time + scan period
		// baseTime must be not null
		if(failureCounter > 1 && ZonedDateTime.now().toInstant().isAfter(baseTime.plusSeconds(scanPeriodInSeconds).toInstant())) {
			isScheduled = true;
		}
		
		return isScheduled;
	}
	
	//Failure scenario
	@Override
	public boolean checkAndUpdateBaseTime(int failureCounter) {
		if(baseTime == null) {
			baseTime = ZonedDateTime.now();
			return true;
		}
		if(failureCounter == 1) {
			baseTime = baseTime.plusSeconds(scanPeriodInSeconds);
			return true;
		}
		return false;
	}
	
}
