package scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Common schedule logic 
 */
public abstract class AbstractScheduler {

	protected LocalDateTime lastRun;
	
	protected int minutesInterval;
	
	public boolean isScheduled() {
		boolean isScheduled = false;
		
		if (lastRun == null) {
			lastRun = LocalDateTime.now();
			isScheduled = true;
		} else {
			LocalDateTime now = LocalDateTime.now();
			if (ChronoUnit.MINUTES.between(lastRun, now) > minutesInterval) {
				lastRun = now;
				isScheduled = true;
			}
		}
		
		return isScheduled;
	}
	
}
