package scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Case1:
 * Defines a rule(point in a time) when slave communication thread requested 
 * transfer. Interval has to be set in minutes.
 * 
 * Case2:
 * Scheduler for slave repository scanning process.
 * The main goal of scanning is to check that each file path from data.repo has 
 * a corresponding file in the slave repository.
 */
public class SlaveScheduler {

	private boolean isScheduled = false;

	private int minutesInterval;

	private LocalDateTime lastRun;
	
	public SlaveScheduler(int minutesInterval) {
		this.minutesInterval = minutesInterval;
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
