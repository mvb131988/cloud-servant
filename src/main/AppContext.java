package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.MasterRepositoryManager;
import file.repository.metadata.RepositoryVisitor;
import file.repository.metadata.SlaveRepositoryManager;
import file.repository.metadata.status.RepositoryStatusMapper;
import protocol.BaseTransferOperations;
import protocol.BatchFilesTransferOperation;
import protocol.FileTransferOperation;
import protocol.FullFileTransferOperation;
import protocol.MasterSlaveCommunicationPool;
import protocol.MasterTransferManager;
import protocol.SlaveTransferManager;
import protocol.StatusTransferOperation;
import protocol.constant.ProtocolStatusMapper;
import protocol.file.FrameProcessor;
import provider.MasterCommunicationProvider;
import transformer.FilesContextTransformer;

public class AppContext {

	private FrameProcessor fp = new FrameProcessor();

	private AppProperties appProperties;

	private Logger logger = LogManager.getRootLogger();
	
	private MasterTransferManager masterTransferManager;
	
	private SlaveTransferManager slaveTransferManager;
	
	private MasterCommunicationProvider masterCommunicationProvider;
	
	private SlaveRepositoryManager slaveRepositoryManager;
	
	private RepositoryStatusMapper repositoryStatusMapper;
	
	private StatusTransferOperation statusTransferOperation;
	
	private BatchFilesTransferOperation batchTransferOperation;
	
	private FullFileTransferOperation fullFileTransferOperation;
	
	private FileTransferOperation fileTransferOperation;
	
	private RepositoryVisitor repositoryVisitor;
	
	private MasterRepositoryManager repositoryManager;
	
	private BaseRepositoryOperations baseRepositoryOperations;
	
	private BaseTransferOperations baseTransferOperations;
	
	//TODO: separate contexts for master/slave
	public AppContext() {
		appProperties = new AppProperties();
		
		baseRepositoryOperations = new BaseRepositoryOperations(getFrameProcessor(), getFilesContextTransformer(), appProperties);
		
		baseTransferOperations = new BaseTransferOperations(getFrameProcessor(), getBaseRepositoryOperations());
		
		repositoryStatusMapper = new RepositoryStatusMapper();
		
		slaveRepositoryManager = new SlaveRepositoryManager(getBaseRepositoryOperations(), getRepositoryStatusMapper());
		
		statusTransferOperation = new StatusTransferOperation(getBaseTransferOperations());
		
		fileTransferOperation = new FileTransferOperation(getBaseTransferOperations(), 
														  getBaseRepositoryOperations(),
														  appProperties);

		batchTransferOperation = new BatchFilesTransferOperation(getFileTransferOperation(),
				 												 getBaseTransferOperations(),
				 												 getFilesContextTransformer(),
				 												 getBaseRepositoryOperations(),
				 												 getSlaveRepositoryManager(),
				 												 getStatusTransferOperation());
		

		fullFileTransferOperation = new FullFileTransferOperation(getFileTransferOperation(),
																  getBaseTransferOperations(),
																  getStatusTransferOperation(),
																  getBatchFilesTransferOperation());
		
		
		masterTransferManager = new MasterTransferManager();
		masterTransferManager.init(getFullFileTransferOperation(), 
								   getStatusTransferOperation(),
								   getSlaveCommunicationPool(), 
								   getProtocolStatusMapper());

		repositoryVisitor = new RepositoryVisitor(appProperties);
		repositoryManager = new MasterRepositoryManager(getRepositoryVisitor(), appProperties);

		masterCommunicationProvider = 
				new MasterCommunicationProvider(getRepositoryManager(), getMasterTransferManager());
		
		slaveTransferManager = new SlaveTransferManager();
		slaveTransferManager.init(getBaseRepositoryOperations(), 
				 				  getFilesContextTransformer(), 
				 				  getFullFileTransferOperation(),
				 				  getStatusTransferOperation());
		
	}
	
	public void start() {
		if (appProperties.isMaster()) {
			startAsServer();
		} else {
			slaveRepositoryManager.init();
			startAsClient();
		}
	}

	private void startAsServer() {
		getMasterCommunicationProvider().init();
	}

	private void startAsClient() {
		SlaveTransferManager stm = getSlaveTransferManager();
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
	
	private BaseRepositoryOperations getBaseRepositoryOperations() {
		return baseRepositoryOperations;
	}

	public RepositoryVisitor getRepositoryVisitor() {
		return repositoryVisitor;
	}

	public SlaveTransferManager getSlaveTransferManager() {
		return slaveTransferManager;
	}
	
	private BaseTransferOperations getBaseTransferOperations() {
		return baseTransferOperations;
	}
	
	public StatusTransferOperation getStatusTransferOperation() {
		return statusTransferOperation;
	}

	private FileTransferOperation getFileTransferOperation() {
		return fileTransferOperation;
	}

	private BatchFilesTransferOperation getBatchFilesTransferOperation() {
		return batchTransferOperation;
	}
	
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
	
	public MasterRepositoryManager getRepositoryManager() {
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
