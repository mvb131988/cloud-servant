package main;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.FilePropertyLookupService;
import file.repository.metadata.RepositoryManager;
import file.repository.metadata.RepositoryVisitor;
import protocol.BaseTransferOperations;
import protocol.BatchFilesTransferOperation;
import protocol.FileTransferOperation;
import protocol.FullFileTransferOperation;
import protocol.MasterTransferManager;
import protocol.SlaveTransferManager;
import protocol.file.FrameProcessor;
import transformer.FilesContextTransformer;

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
		getMasterTransferManager().init(getBatchFilesTransferOperation(), getFullFileTransferOperation());
	}

	private void startAsClient() {
		SlaveTransferManager stm = getSlaveTransferManager();
		stm.init(getBatchFilesTransferOperation(), getBaseRepositoryOperations(), getFilesContextTransformer(), getFullFileTransferOperation());
		stm.getSlaveTransferThread().start();
	}

	// // prototype scope
	// public RepositoryManager getRepositoryManager() {
	// return new RepositoryManager();
	// }

	private FrameProcessor frameProcessor = new FrameProcessor();
	private FrameProcessor getFrameProcessor() {
		return frameProcessor;
	}
	
	private FilesContextTransformer filesContextTransformer = new FilesContextTransformer();
	private FilesContextTransformer getFilesContextTransformer() {
		return filesContextTransformer;
	}
	
	private BaseRepositoryOperations baseRepositoryOperations = new BaseRepositoryOperations(frameProcessor);
	private BaseRepositoryOperations getBaseRepositoryOperations() {
		return baseRepositoryOperations;
	}
	

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
	
	private FilePropertyLookupService fpls = new FilePropertyLookupService();
	private FilePropertyLookupService getFilePropertyLookupService() {
		return fpls;
	}
	
	private BaseTransferOperations baseTransferOperations = new BaseTransferOperations(getFrameProcessor());
	private BaseTransferOperations getBaseTransferOperations() {
		return baseTransferOperations;
	}
	
	private FileTransferOperation fileTransferOperation = new FileTransferOperation(getBaseTransferOperations(), 
																					getFilePropertyLookupService());
	private FileTransferOperation getFileTransferOperation() {
		return fileTransferOperation;
	}

	private BatchFilesTransferOperation batchTransferOperation = new BatchFilesTransferOperation(getFileTransferOperation(),
																								 getBaseTransferOperations());
	private BatchFilesTransferOperation getBatchFilesTransferOperation() {
		return batchTransferOperation;
	}
	
	private FullFileTransferOperation fullFileTransferOperation = new FullFileTransferOperation(getFileTransferOperation(),
			 																					getBaseTransferOperations(),
			 																					getBaseRepositoryOperations(),
			 																					getFilesContextTransformer());
	private FullFileTransferOperation getFullFileTransferOperation() {
		return fullFileTransferOperation;
	}
	
	/**
	 * repository.metadata
	 */

	private RepositoryManager repositoryManager = new RepositoryManager();

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	

}
