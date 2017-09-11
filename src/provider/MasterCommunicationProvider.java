package provider;

import file.repository.metadata.RepositoryManager;
import file.repository.metadata.RepositoryManager.RepositoryScaner;
import file.repository.metadata.RepositoryScannerStatus;
import protocol.MasterTransferManager;
import protocol.MasterTransferManager.MasterTransferThread;

public class MasterCommunicationProvider {

	private RepositoryManager repositoryManager;

	private MasterTransferManager masterTransferManager;

	private Thread masterCommunicationProviderThread;

	public MasterCommunicationProvider(RepositoryManager repositoryManager,
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
		masterCommunicationProviderThread.start();
	}

	private class MasterCommunicationProviderThread implements Runnable {

		@Override
		public void run() {
			boolean isStartup = true;
			//TODO: Define schedule rules
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
					t2.start();
					isStartup = false;
				} 
				else if (isScheduled) {
					mtt.pause();
					
					repositoryScaner.reset();
					t1.start();
					
					isScheduled = false;
				}
				
				//As soon as repository was scanned and data.repo created make all waiting communications
				//ready for transfer
				if (repositoryScaner.getStatus() == RepositoryScannerStatus.READY) {
					mtt.resume();
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

	}

}
