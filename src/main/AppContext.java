package main;

import file.repository.metadata.RepositoryManager;
import file.repository.metadata.RepositoryVisitor;
import protocol.BaseTransferOperations;
import protocol.FileTransferOperation;
import protocol.MasterTransferManager;
import protocol.SlaveTransferManager;
import protocol.file.FrameProcessor;

public class AppContext {

	private FrameProcessor fp = new FrameProcessor();

	private boolean isMaster = true;

	public void start() {
		if (isMaster) {
			startAsServer();
		} else {
			startAsClient();
		}
	}

	private void startAsServer() {
		// scan repository and create data.repository
		Thread repositoryScaner = getRepositoryManager().getScaner();
		repositoryScaner.start();
		try {
			repositoryScaner.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// only after scan repository thread is finished start master file
		// transferring component
		getMasterTransferManager().init(getFileTransferOperation());
	}

	private void startAsClient() {
		SlaveTransferManager stm = getSlaveTransferManager();
		stm.init(getFileTransferOperation());
		stm.getSlaveTransferThread().start();
	}

	// // prototype scope
	// public RepositoryManager getRepositoryManager() {
	// return new RepositoryManager();
	// }

	// singleton scope
	private RepositoryVisitor repositoryVisitor = new RepositoryVisitor();

	public RepositoryVisitor getRepositoryVisitor() {
		return repositoryVisitor;
	}

	private MasterTransferManager masterTransferManager = new MasterTransferManager();

	public MasterTransferManager getMasterTransferManager() {
		return masterTransferManager;
	}

	private SlaveTransferManager slaveTransferManager = new SlaveTransferManager();

	public SlaveTransferManager getSlaveTransferManager() {
		return slaveTransferManager;
	}

	/**
	 * repository.metadata
	 */

	private RepositoryManager repositoryManager = new RepositoryManager();

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}
	
	private FileTransferOperation fileTransferOperation = new FileTransferOperation(getBaseTransferOperations());
	
	private FileTransferOperation getFileTransferOperation() {
		return fileTransferOperation;
	}
	
	private BaseTransferOperations baseTransferOperations = new BaseTransferOperations(getFrameProcessor());
	private BaseTransferOperations getBaseTransferOperations() {
		return baseTransferOperations;
	}
	
	private FrameProcessor frameProcessor = new FrameProcessor();
	private FrameProcessor getFrameProcessor() {
		return frameProcessor;
	}

}
