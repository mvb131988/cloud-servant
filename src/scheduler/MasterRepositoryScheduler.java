package scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import main.AppProperties;

/**
 * Defines a rule(point in a time) when repository scan process
 * occurs.
 * Interval has to be set in minutes.  
 */
public class MasterRepositoryScheduler {

	protected int minutesInterval;

	protected LocalDateTime lastRun;
	
	public MasterRepositoryScheduler(AppProperties appProperties) {
		minutesInterval = appProperties.getMasterRepositoryScheduleInterval();
	}
	
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
