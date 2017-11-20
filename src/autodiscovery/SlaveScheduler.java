package autodiscovery;

/**
 * Determines if autodiscovery process has to be started 
 */
public interface SlaveScheduler {

	boolean isScheduled(int failureCounter, String masterIp);
	
	boolean checkAndUpdateBaseTime(int failureCounter);

	void updateBaseTime();
	
}
