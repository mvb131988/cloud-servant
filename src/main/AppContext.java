package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.FilePropertyLookupService;
import file.repository.metadata.RepositoryManager;
import file.repository.metadata.RepositoryScannerStatus;
import file.repository.metadata.RepositoryVisitor;
import file.repository.metadata.SlaveRepositoryManager;
import file.repository.metadata.status.RepositoryStatusMapper;
import protocol.BaseTransferOperations;
import protocol.BatchFilesTransferOperation;
import protocol.FileTransferOperation;
import protocol.FullFileTransferOperation;
import protocol.MasterTransferManager;
import protocol.MasterSlaveCommunicationPool;
import protocol.SlaveTransferManager;
import protocol.StatusTransferOperation;
import protocol.constant.ProtocolStatusMapper;
import protocol.file.FrameProcessor;
import provider.MasterCommunicationProvider;
import transformer.FilesContextTransformer;

public class AppContext {

	private FrameProcessor fp = new FrameProcessor();

	private boolean isMaster = true;

	private Logger logger = LogManager.getRootLogger();
	
	private MasterTransferManager masterTransferManager;
	
	private MasterCommunicationProvider masterCommunicationProvider;
	
	private SlaveRepositoryManager slaveRepositoryManager;
	
	private RepositoryStatusMapper repositoryStatusMapper;
	
	public AppContext() {
		masterTransferManager = new MasterTransferManager();
		masterTransferManager.init(getFullFileTransferOperation(), 
								   getStatusTransferOperation(),
								   getSlaveCommunicationPool(), 
								   getProtocolStatusMapper());
		
		masterCommunicationProvider = 
				new MasterCommunicationProvider(getRepositoryManager(), getMasterTransferManager());
		
		repositoryStatusMapper = new RepositoryStatusMapper();
		slaveRepositoryManager = new SlaveRepositoryManager(getBaseRepositoryOperations(), getRepositoryStatusMapper());
	}
	
	public void start() {
		if (isMaster) {
			startAsServer();
		} else {
			startAsClient();
		}
	}

	private void startAsServer() {
		getMasterCommunicationProvider().init();
	}

	private void startAsClient() {
		SlaveTransferManager stm = getSlaveTransferManager();
		stm.init(getBaseRepositoryOperations(), 
				 getFilesContextTransformer(), 
				 getFullFileTransferOperation(),
				 getStatusTransferOperation());
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
	
	private FilesContextTransformer filesContextTransformer = new FilesContextTransformer(getFrameProcessor());
	private FilesContextTransformer getFilesContextTransformer() {
		return filesContextTransformer;
	}
	
	private BaseRepositoryOperations baseRepositoryOperations = new BaseRepositoryOperations(getFrameProcessor(), getFilesContextTransformer());
	private BaseRepositoryOperations getBaseRepositoryOperations() {
		return baseRepositoryOperations;
	}

	// singleton scope
	private RepositoryVisitor repositoryVisitor = new RepositoryVisitor();

	public RepositoryVisitor getRepositoryVisitor() {
		return repositoryVisitor;
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
	
	private StatusTransferOperation sto = new StatusTransferOperation(getBaseTransferOperations());
	public StatusTransferOperation getStatusTransferOperation() {
		return sto;
	}

	private FileTransferOperation fileTransferOperation = new FileTransferOperation(getBaseTransferOperations(), 
																					getFilePropertyLookupService());
	private FileTransferOperation getFileTransferOperation() {
		return fileTransferOperation;
	}

	private BatchFilesTransferOperation batchTransferOperation = new BatchFilesTransferOperation(getFileTransferOperation(),
																								 getBaseTransferOperations(),
																								 getFilesContextTransformer(),
																								 getBaseRepositoryOperations(),
																								 getSlaveRepositoryManager(),
																								 getStatusTransferOperation());
	private BatchFilesTransferOperation getBatchFilesTransferOperation() {
		return batchTransferOperation;
	}
	
	private FullFileTransferOperation fullFileTransferOperation = new FullFileTransferOperation(getFileTransferOperation(),
			 																					getBaseTransferOperations(),
			 																					getBaseRepositoryOperations(),
			 																					getFilesContextTransformer(),
			 																					getStatusTransferOperation(),
			 																					getBatchFilesTransferOperation());
	private FullFileTransferOperation getFullFileTransferOperation() {
		return fullFileTransferOperation;
	}
	
	private MasterSlaveCommunicationPool slaveCommunicationPool = new MasterSlaveCommunicationPool();
	private MasterSlaveCommunicationPool getSlaveCommunicationPool() {
		return slaveCommunicationPool;
	}
	
	private ProtocolStatusMapper protocolStatusMapper = new ProtocolStatusMapper();
	private ProtocolStatusMapper getProtocolStatusMapper() {
		return protocolStatusMapper;
	}
	
	/**
	 * repository.metadata
	 */

	private RepositoryManager repositoryManager = new RepositoryManager();

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}
	
	public MasterTransferManager getMasterTransferManager() {
		return masterTransferManager;
	}

	public MasterCommunicationProvider getMasterCommunicationProvider() {
		return masterCommunicationProvider;
	}

	public SlaveRepositoryManager getSlaveRepositoryManager() {
		return slaveRepositoryManager;
	}

	public RepositoryStatusMapper getRepositoryStatusMapper() {
		return repositoryStatusMapper;
	}
	
}
