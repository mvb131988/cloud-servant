package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.SlaveAutodiscoverer;
import autodiscovery.SlaveAutodiscoveryAdapter;
import autodiscovery.SlaveGlobalAutodiscoverer;
import autodiscovery.SlaveLocalAutodiscoverer;
import autodiscovery.SlaveLocalScheduler;
import ipscanner.IpRangeAnalyzer;
import ipscanner.IpRangesAnalyzer;
import ipscanner.IpScanner;
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
import transformer.IntegerTransformer;
import transformer.LongTransformer;

public class AppContext {

	private Logger logger = LogManager.getRootLogger();

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
	
	private IntegerTransformer integerTransformer;
	
	private LongTransformer longTransformer;
	
	private MasterRepositoryScheduler masterRepositoryScheduler;
	
	private SlaveTransferScheduler slaveTransferScheduler;
	
	private MasterShutdownThread masterShutdownThread;
	
	private IpScanner ipScanner;
	
	private SlaveAutodiscoveryAdapter slaveAutodiscoveryAdapter;
	
	private SlaveLocalAutodiscoverer localDiscoverer;
	
	private SlaveGlobalAutodiscoverer globalDiscoverer;
	
	//============================================================
	//	Prototypes. Classes with states go here 
	//============================================================
	public SlaveLocalScheduler getSlaveLocalScheduler() {
		return new SlaveLocalScheduler(appProperties);
	}
	
	public SlaveAutodiscoverer getDiscoverer() {
		return new SlaveAutodiscoverer(getLocalDiscoverer());
	}
	
	public IpRangeAnalyzer getIpRangeAnalyzer() {
		return new IpRangeAnalyzer();
	}
	
	public IpRangesAnalyzer getIpRangesAnalyzer() {
		return new IpRangesAnalyzer(getIpRangeAnalyzer());
	}
	
	//============================================================
	
	public void initAsMaster() {
		//Separate thread intended for (master-side) application shutdown
		masterShutdownThread = new MasterShutdownThread(appProperties);
		masterShutdownThread.start();
		
		//Others
		longTransformer = new LongTransformer();
		integerTransformer = new IntegerTransformer();
		protocolStatusMapper = new ProtocolStatusMapper();
		filesContextTransformer = new FilesContextTransformer(getLongTransformer());
		
		//Repository operations
		repositoryVisitor = new RepositoryVisitor(appProperties);
		baseRepositoryOperations = new BaseRepositoryOperations(getLongTransformer(), getFilesContextTransformer(), appProperties);
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getIntegerTransformer(),
															getLongTransformer(), 
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
		masterRepositoryManager.init();
		
		masterRepositoryScheduler = new MasterRepositoryScheduler(appProperties);
		
		//Top layer: layer 0 
		masterCommunicationProvider = new MasterCommunicationProvider(getMasterRepositoryManager(), 
																	  getMasterTransferManager(), 
																	  getMasterRepositoryScheduler(),
																	  appProperties);
	}
	
	public void initAsSlave() {
		//autodiscovering
		ipScanner = new IpScanner(getIpRangesAnalyzer(), appProperties);
		globalDiscoverer = new SlaveGlobalAutodiscoverer(appProperties);
		localDiscoverer = new SlaveLocalAutodiscoverer(getGlobalDiscoverer(), getSlaveLocalScheduler(), getIpScanner(), appProperties);
		slaveAutodiscoveryAdapter = new SlaveAutodiscoveryAdapter(getDiscoverer(), appProperties);
		
		//Others
		longTransformer = new LongTransformer();
		integerTransformer = new IntegerTransformer();
		protocolStatusMapper = new ProtocolStatusMapper();
		filesContextTransformer = new FilesContextTransformer(getLongTransformer());
		
		//Repository operations
		repositoryStatusMapper = new RepositoryStatusMapper();
		repositoryVisitor = new RepositoryVisitor(appProperties);
		baseRepositoryOperations = new BaseRepositoryOperations(getLongTransformer(), getFilesContextTransformer(), appProperties);
		slaveRepositoryManager = new SlaveRepositoryManager(getBaseRepositoryOperations(), 
															getRepositoryStatusMapper());
		slaveRepositoryManager.init();
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getIntegerTransformer(),
															getLongTransformer(), 
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
				 				  getSlaveAutodiscoveryAdapter(),
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

	private IntegerTransformer getIntegerTransformer() {
		return integerTransformer;
	}
	
	private LongTransformer getLongTransformer() {
		return longTransformer;
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

	public IpScanner getIpScanner() {
		return ipScanner;
	}

	public SlaveAutodiscoveryAdapter getSlaveAutodiscoveryAdapter() {
		return slaveAutodiscoveryAdapter;
	}

	public SlaveLocalAutodiscoverer getLocalDiscoverer() {
		return localDiscoverer;
	}

	public SlaveGlobalAutodiscoverer getGlobalDiscoverer() {
		return globalDiscoverer;
	}
	
}
