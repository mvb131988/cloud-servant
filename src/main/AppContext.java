package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import provider.MasterCommunicationProvider;
import repository.BaseRepositoryOperations;
import repository.MasterRepositoryManager;
import repository.RepositoryVisitor;
import repository.SlaveRepositoryManager;
import repository.status.RepositoryStatusMapper;
import scheduler.MasterRepositoryScheduler;
import scheduler.SlaveTransferScheduler;
import transfer.BaseTransferOperations;
import transfer.BatchFilesTransferOperation;
import transfer.FileTransferOperation;
import transfer.FullFileTransferOperation;
import transfer.HealthCheckOperation;
import transfer.MasterSlaveCommunicationPool;
import transfer.MasterTransferManager;
import transfer.SlaveTransferManager;
import transfer.StatusAndHealthCheckOperation;
import transfer.StatusTransferOperation;
import transfer.constant.ProtocolStatusMapper;
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
	
	private HealthCheckOperation healthCheckOperation;
	
	private StatusAndHealthCheckOperation statusAndHealthCheckOperation; 
	
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
	
	private MasterRepositoryScheduler masterRepositoryScheduler;
	
	private SlaveTransferScheduler slaveTransferScheduler;
	
	private MasterShutdownThread masterShutdownThread;
	
	public void initAsMaster() {
		//Separate thread intended for (master-side) application shutdown
		masterShutdownThread = new MasterShutdownThread(appProperties);
		masterShutdownThread.start();
		
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
		healthCheckOperation = new HealthCheckOperation(getBaseTransferOperations());
		statusAndHealthCheckOperation = new StatusAndHealthCheckOperation(getBaseTransferOperations(),
																		  getHealthCheckOperation(), 
																		  getStatusTransferOperation());
		batchTransferOperation = new BatchFilesTransferOperation(getBaseTransferOperations(),
																 getFileTransferOperation(),
																 getStatusTransferOperation(),
																 null,
																 getFilesContextTransformer(),
																 appProperties);
		fullFileTransferOperation = new FullFileTransferOperation(getBaseTransferOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getHealthCheckOperation(),
																  getBatchFilesTransferOperation());
		
		//Layer 1
		masterSlaveCommunicationPool = new MasterSlaveCommunicationPool();
		masterTransferManager = new MasterTransferManager(appProperties);
		masterTransferManager.init(getFullFileTransferOperation(), 
								   getStatusTransferOperation(),
								   getHealthCheckOperation(),
								   getStatusAndHealthCheckOperation(),
								   getMasterSlaveCommunicationPool(), 
								   getProtocolStatusMapper(),
								   appProperties);
		masterRepositoryManager = new MasterRepositoryManager(getRepositoryVisitor(), getBaseRepositoryOperations(), appProperties);

		masterRepositoryScheduler = new MasterRepositoryScheduler(appProperties);
		
		//Top layer: layer 0 
		masterCommunicationProvider = new MasterCommunicationProvider(getMasterRepositoryManager(), 
																	  getMasterTransferManager(), 
																	  getMasterRepositoryScheduler(),
																	  appProperties);
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
		healthCheckOperation = new HealthCheckOperation(getBaseTransferOperations());
		batchTransferOperation = new BatchFilesTransferOperation(getBaseTransferOperations(),
				 												 getFileTransferOperation(),
				 												 getStatusTransferOperation(),
				 												 getSlaveRepositoryManager(),
				 												 getFilesContextTransformer(),
				 												 appProperties);
		fullFileTransferOperation = new FullFileTransferOperation(getBaseTransferOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getHealthCheckOperation(),
																  getBatchFilesTransferOperation());
		slaveTransferScheduler = new SlaveTransferScheduler(appProperties);
		
		//Top layer: layer 0 
		slaveTransferManager = new SlaveTransferManager(appProperties);
		slaveTransferManager.init(getFullFileTransferOperation(),
				 				  getStatusTransferOperation(),
				 				  getHealthCheckOperation(),
				 				  getSlaveTransferScheduler(),
				 				  appProperties);
	}
	
	public void start(AppProperties appProperties) {
		this.appProperties = appProperties;
		if (this.appProperties.isMaster()) {
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

	public MasterRepositoryScheduler getMasterRepositoryScheduler() {
		return masterRepositoryScheduler;
	}

	public SlaveTransferScheduler getSlaveTransferScheduler() {
		return slaveTransferScheduler;
	}

	public HealthCheckOperation getHealthCheckOperation() {
		return healthCheckOperation;
	}

	public StatusAndHealthCheckOperation getStatusAndHealthCheckOperation() {
		return statusAndHealthCheckOperation;
	}
	
}
