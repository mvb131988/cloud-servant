package provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;
import repository.MasterRepositoryManager;
import repository.RepositoryScannerStatus;
import repository.MasterRepositoryManager.RepositoryScaner;
import scheduler.MasterRepositoryScheduler;
import transfer.MasterTransferManager;
import transfer.MasterTransferManager.MasterTransferThread;

/**
 * Synchronize repository scanner with master transfer manager.
 * When repository scanner is working, all communications holding by master transfer manager must be in BUSY state(
 * meaning no transfer could occur during the scan).  
 */
public class MasterCommunicationProvider {

	private final int bigTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private MasterRepositoryManager repositoryManager;

	private MasterTransferManager masterTransferManager;

	private Thread masterCommunicationProviderThread;
	
	private MasterRepositoryScheduler masterRepositoryScheduler;

	public MasterCommunicationProvider(MasterRepositoryManager repositoryManager,
									   MasterTransferManager masterTransferManager,
									   MasterRepositoryScheduler masterRepositoryScheduler,
									   AppProperties appProperties) 
	{
		super();
		this.repositoryManager = repositoryManager;
		this.masterTransferManager = masterTransferManager;
		this.masterRepositoryScheduler = masterRepositoryScheduler;
		this.bigTimeout = appProperties.getBigPoolingTimeout();
	}

	/**
	 * Execute it just after constructor
	 */
	public void init() {
		masterCommunicationProviderThread = new Thread(new MasterCommunicationProviderThread());
		masterCommunicationProviderThread.setName("MasterCommunicationProviderThread");
		masterCommunicationProviderThread.start();
	}

	private class MasterCommunicationProviderThread implements Runnable {

		@Override
		public void run() {
			try {
				logger.info("[" + this.getClass().getSimpleName() + "] started");
				
				boolean isStartup = true;
	
				RepositoryScaner repositoryScaner = repositoryManager.getScaner();
				MasterTransferThread mtt = masterTransferManager.getMasterTransferThread();
	
				Thread t1 = new Thread(repositoryScaner);
				t1.setName("RepositoryScaner");
				Thread t2 = new Thread(mtt);
				t2.setName("MasterTransferThread");
	
				for (;;) {
					// startup/schedule part
					if (isStartup) {
						t1.start();
						repositoryScaner.reset();
						
						t2.start();
						isStartup = false;
					} 
					else if (repositoryScaner.getStatus() == RepositoryScannerStatus.READY && masterRepositoryScheduler.isScheduled()) {
						logger.info("[" + this.getClass().getSimpleName() + "] Master repository scan is scheduled");
						mtt.pause();
						repositoryScaner.reset();
					}
					
					//As soon as repository was scanned and data.repo created make all waiting communications
					//ready for transfer
					if (repositoryScaner.getStatus() == RepositoryScannerStatus.READY) {
						logger.info("[" + this.getClass().getSimpleName() + "] Master repository scan is done");
						mtt.resume();
					}
					
					//One iteration of the loop in a minute is sufficient for synchronization purpose.
					//Wait 1 minute to avoid resources overconsumption.
					Thread.sleep(bigTimeout);
				}
			} catch(Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail. Master application is in wrong state.", e);
			}
		}

	}

}
