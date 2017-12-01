package autodiscovery;

/**
 * Determines if autodiscovery process has to be started 
 */
public interface SlaveScheduler {

	boolean isScheduled(int failureCounter);
	
	boolean checkAndUpdateBaseTime(int failureCounter);

	void updateBaseTime();
	
}
