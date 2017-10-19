package scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import main.AppProperties;

/**
 * Defines a rule(point in a time) when slave communication thread requested 
 * transfer.
 * Interval has to be set in minutes.  
 */
public class SlaveTransferScheduler {

	private boolean isScheduled = false;

	private int minutesInterval;

	private LocalDateTime lastRun;
	
	public SlaveTransferScheduler(AppProperties appProperties) {
		minutesInterval = appProperties.getSlaveTransferScheduleInterval();
	}
	
	public boolean isScheduled() {
		if (lastRun == null) {
			isScheduled = true;
		}
		else {
			LocalDateTime now = LocalDateTime.now();
			if (ChronoUnit.MINUTES.between(lastRun, now) > minutesInterval) {
				isScheduled = true;
			}
		}
		return isScheduled;
	}
	
	public void scheduleNext() {
		lastRun = LocalDateTime.now();
	}
	
}
