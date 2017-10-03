package provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.MasterRepositoryManager;
import repository.RepositoryScannerStatus;
import repository.MasterRepositoryManager.RepositoryScaner;
import transfer.MasterTransferManager;
import transfer.MasterTransferManager.MasterTransferThread;

/**
 * Synchronize repository scanner with master transfer manager.
 * When repository scanner is working, all communications holding by master transfer manager must be in BUSY state(
 * meaning no transfer could occur during the scan).  
 */
public class MasterCommunicationProvider {

	private Logger logger = LogManager.getRootLogger();
	
	private MasterRepositoryManager repositoryManager;

	private MasterTransferManager masterTransferManager;

	private Thread masterCommunicationProviderThread;

	public MasterCommunicationProvider(MasterRepositoryManager repositoryManager,
			MasterTransferManager masterTransferManager) {
		super();
		this.repositoryManager = repositoryManager;
		this.masterTransferManager = masterTransferManager;
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
				//TODO(NORMAL): Define schedule rules
				boolean isScheduled = false;
	
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
					else if (isScheduled && repositoryScaner.getStatus() == RepositoryScannerStatus.READY) {
						mtt.pause();
						
						repositoryScaner.reset();
						
						isScheduled = false;
					}
					
					//As soon as repository was scanned and data.repo created make all waiting communications
					//ready for transfer
					if (repositoryScaner.getStatus() == RepositoryScannerStatus.READY) {
						mtt.resume();
					}
					
					Thread.sleep(30000);
				}
			} catch(Exception e) {
				//TODO: Log exception
			}
		}

	}

}
