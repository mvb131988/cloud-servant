package scheduler;

import main.AppProperties;

/**
 * Defines a rule(point in a time) when repository scan process
 * occurs.
 * Interval has to be set in minutes.  
 */
public class MasterRepositoryScheduler extends AbstractScheduler {

	public MasterRepositoryScheduler(AppProperties appProperties) {
		minutesInterval = appProperties.getMasterRepositoryScheduleInterval();
	}
	
}
