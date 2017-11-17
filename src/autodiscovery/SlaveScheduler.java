package autodiscovery;

public interface SlaveScheduler {

	boolean isScheduled(int failureCounter, String masterIp);
	
	boolean checkAndUpdateBaseTime(int failureCounter);
	
}
