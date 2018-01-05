package provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;
import scheduler.SlaveScheduler;
import transfer.SlaveTransferManager;
import transfer.SlaveTransferManager.SlaveTransferThread;

public class SlaveCommunicationProvider {

	private final int bigTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private SlaveTransferManager slaveTransferManager;
	
	private Thread slaveCommunicationProviderThread;
	
	// Scheduler for repository status check (repository scan and check)
	private SlaveScheduler repositoryScheduler;
	
	public SlaveCommunicationProvider(SlaveTransferManager slaveTransferManager,
									  SlaveScheduler scheduler,
								      AppProperties appProperties) 
	{
		super();
		this.slaveTransferManager = slaveTransferManager;
		this.repositoryScheduler = scheduler;
		this.bigTimeout = appProperties.getBigPoolingTimeout();
	}
	
	public void init() {
		slaveCommunicationProviderThread = new Thread(new SlaveCommunicationProviderThread());
		slaveCommunicationProviderThread.setName("SlaveCommunicationProviderThread");
		slaveCommunicationProviderThread.start();
	}
	
	private class SlaveCommunicationProviderThread implements Runnable {

		@Override
		public void run() {
			try {
				SlaveTransferThread slaveTransferThread = slaveTransferManager.getSlaveTransferThread();

				Thread t = new Thread(slaveTransferThread);
				t.setName("SlaveTransferThread");
				t.start();

				for (;;) {
					// Repository status check (repository scan)
					if(repositoryScheduler.isScheduled()) {
						slaveTransferThread.pause();
						
						//scan and create repo status file
						
						repositoryScheduler.scheduleNext();
					}
					
					slaveTransferThread.resume();

					// One iteration of the loop in a minute is sufficient for
					// synchronization purpose.
					// Wait 1 minute to avoid resources overconsumption.
					Thread.sleep(bigTimeout);
				}
			} catch (Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail. Slave application is in wrong state.", e);
			}
		}
		
	}
}
