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
import provider.MasterCommunicationProvider;
import transformer.FilesContextTransformer;
import transformer.LongTransformer;

public class AppContext {

	private Logger logger = LogManager.getRootLogger();

	private LongTransformer fp = new LongTransformer();

	private AppProperties appProperties;
	
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
	
	private MasterRepositoryManager masterRepositoryManager;
	
	private BaseRepositoryOperations baseRepositoryOperations;
	
	private BaseTransferOperations baseTransferOperations;
	
	private MasterSlaveCommunicationPool masterSlaveCommunicationPool;
	
	private ProtocolStatusMapper protocolStatusMapper;
	
	private FilesContextTransformer filesContextTransformer;
	
	private LongTransformer frameProcessor;
	
	public void initAsMaster() {
		//Others
		frameProcessor = new LongTransformer();
		protocolStatusMapper = new ProtocolStatusMapper();
		filesContextTransformer = new FilesContextTransformer(getFrameProcessor());
		
		//Repository operations
		repositoryVisitor = new RepositoryVisitor(appProperties);
		baseRepositoryOperations = new BaseRepositoryOperations(getFrameProcessor(), getFilesContextTransformer(), appProperties);
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getFrameProcessor(), 
															getBaseRepositoryOperations());
		fileTransferOperation = new FileTransferOperation(getBaseTransferOperations(), 
														  getBaseRepositoryOperations(),
														  appProperties);
		statusTransferOperation = new StatusTransferOperation(getBaseTransferOperations());
		batchTransferOperation = new BatchFilesTransferOperation(getBaseTransferOperations(),
																 getFileTransferOperation(),
																 getStatusTransferOperation(),
																 null,
																 getFilesContextTransformer());
		
		fullFileTransferOperation = new FullFileTransferOperation(getBaseTransferOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getBatchFilesTransferOperation());
		
		//Layer 1
		masterSlaveCommunicationPool = new MasterSlaveCommunicationPool();
		masterTransferManager = new MasterTransferManager();
		masterTransferManager.init(getFullFileTransferOperation(), 
								   getStatusTransferOperation(),
								   getMasterSlaveCommunicationPool(), 
								   getProtocolStatusMapper());
		masterRepositoryManager = new MasterRepositoryManager(getRepositoryVisitor(), getBaseRepositoryOperations(), appProperties);
		
		//Top layer: layer 0 
		masterCommunicationProvider = 
				new MasterCommunicationProvider(getMasterRepositoryManager(), getMasterTransferManager());
	}
	
	public void initAsSlave() {
		//Others
		frameProcessor = new LongTransformer();
		protocolStatusMapper = new ProtocolStatusMapper();
		filesContextTransformer = new FilesContextTransformer(getFrameProcessor());
		
		//Repository operations
		repositoryStatusMapper = new RepositoryStatusMapper();
		repositoryVisitor = new RepositoryVisitor(appProperties);
		baseRepositoryOperations = new BaseRepositoryOperations(getFrameProcessor(), getFilesContextTransformer(), appProperties);
		slaveRepositoryManager = new SlaveRepositoryManager(getBaseRepositoryOperations(), 
															getRepositoryStatusMapper());
		slaveRepositoryManager.init();
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getFrameProcessor(), 
															getBaseRepositoryOperations());
		fileTransferOperation = new FileTransferOperation(getBaseTransferOperations(), 
														  getBaseRepositoryOperations(),
														  appProperties);
		statusTransferOperation = new StatusTransferOperation(getBaseTransferOperations());
		batchTransferOperation = new BatchFilesTransferOperation(getBaseTransferOperations(),
				 												 getFileTransferOperation(),
				 												 getStatusTransferOperation(),
				 												 getSlaveRepositoryManager(),
				 												 getFilesContextTransformer());
		fullFileTransferOperation = new FullFileTransferOperation(getBaseTransferOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getBatchFilesTransferOperation());
		
		
		//Top layer: layer 0 
		slaveTransferManager = new SlaveTransferManager();
		slaveTransferManager.init(getBaseRepositoryOperations(), 
				 				  getFilesContextTransformer(), 
				 				  getFullFileTransferOperation(),
				 				  getStatusTransferOperation());
	}
	
	public void start() {
		appProperties = new AppProperties();
		if (appProperties.isMaster()) {
			initAsMaster();
			getMasterCommunicationProvider().init();
		} else {
			initAsSlave();
			getSlaveTransferManager().getSlaveTransferThread().start();
		}
	}

	private LongTransformer getFrameProcessor() {
		return frameProcessor;
	}
	
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
	
	private MasterSlaveCommunicationPool getMasterSlaveCommunicationPool() {
		return masterSlaveCommunicationPool;
	}
	
	private ProtocolStatusMapper getProtocolStatusMapper() {
		return protocolStatusMapper;
	}
	
	public MasterRepositoryManager getMasterRepositoryManager() {
		return masterRepositoryManager;
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
