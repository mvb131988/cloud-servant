package autodiscovery;

/**
 * Determines if autodiscovery process has to be started 
 */
//TODO: name collision with scheduler.SlaveScheduler
public interface SlaveScheduler {

	boolean isScheduled(int failureCounter);
	
	boolean checkAndUpdateBaseTime(int failureCounter);

	void updateBaseTime();
	
}
