package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.IpAutodiscoverer;
import autodiscovery.MemberIpMonitor;
import autodiscovery.MemberType;
import autodiscovery.SlaveAutodiscoveryScheduler;
import autodiscovery.SlaveGlobalAutodiscoverer;
import autodiscovery.SlaveLocalAutodiscoverer;
import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpRangeAnalyzer;
import autodiscovery.ipscanner.IpRangesAnalyzer;
import autodiscovery.ipscanner.IpValidator;
import exception.InitializationException;
import repository.BaseRepositoryOperations;
import repository.RepositoryManager;
import repository.RepositoryManager.RepositoryScaner;
import repository.RepositoryVisitor;
import repository.SlaveRepositoryManager;
import repository.status.RepositoryStatusMapper;
import scheduler.MasterRepositoryScheduler;
import scheduler.SlaveScheduler;
import transfer.BaseTransferOperations;
import transfer.BatchFilesTransferOperation;
import transfer.FileTransferOperation;
import transfer.FullFileTransferOperation;
import transfer.HealthCheckOperation;
import transfer.InboundTransferManager;
import transfer.OutboundTransferManager;
import transfer.StatusAndHealthCheckOperation;
import transfer.StatusTransferOperation;
import transfer.TransferManagerStateMonitor;
import transfer.constant.ProtocolStatusMapper;
import transformer.FilesContextTransformer;
import transformer.IntegerTransformer;
import transformer.LongTransformer;

public class AppContext {

	private Logger logger = LogManager.getRootLogger();

	private AppProperties appProperties;
	
	private SlaveRepositoryManager slaveRepositoryManager;
	
	private RepositoryStatusMapper repositoryStatusMapper;
	
	private StatusTransferOperation statusTransferOperation;
	
	private HealthCheckOperation healthCheckOperation;
	
	private StatusAndHealthCheckOperation statusAndHealthCheckOperation; 
	
	private BatchFilesTransferOperation batchTransferOperation;
	
	private FullFileTransferOperation fullFileTransferOperation;
	
	private FileTransferOperation fileTransferOperation;
	
	private RepositoryVisitor repositoryVisitor;
	
	private RepositoryManager repositoryManager;
	
	private BaseRepositoryOperations baseRepositoryOperations;
	
	private BaseTransferOperations baseTransferOperations;
	
	private ProtocolStatusMapper protocolStatusMapper;
	
	private FilesContextTransformer filesContextTransformer;
	
	private IntegerTransformer integerTransformer;
	
	private LongTransformer longTransformer;
	
	private MasterRepositoryScheduler masterRepositoryScheduler;
	
	private MasterShutdownThread masterShutdownThread;
	
	private AppInitializer appInitializer;
	
	private IpValidator ipValidator;
	
	private IpAutodiscoverer ipAutodiscoverer;
	
	private MemberIpMonitor memberIpMonitor;
	
	private InboundTransferManager inboundTransferManager;
	
	private OutboundTransferManager outboundTransferManager;
	
	private TransferManagerStateMonitor transferManagerStateMonitor;
	
	//============================================================
	//	Prototypes. Classes with states go here 
	//============================================================
	public IpRangeAnalyzer getIpRangeAnalyzer() {
		return new IpRangeAnalyzer();
	}
	
	public IpRangesAnalyzer getIpRangesAnalyzer() {
		return new IpRangesAnalyzer(getIpRangeAnalyzer());
	}
	
	public IpFJPScanner getLocalIpFJPScanner() {
		return new IpFJPScanner(getBaseRepositoryOperations(), 
								getIpValidator(), 
								getIpRangesAnalyzer(), 
								appProperties.getLocalWorkPerThread(), 
								appProperties);
	}
	
	public IpFJPScanner getGlobalIpFJPScanner() {
		return new IpFJPScanner(getBaseRepositoryOperations(), 
								getIpValidator(), 
								getIpRangesAnalyzer(), 
								appProperties.getGlobalWorkPerThread(), 
								appProperties);
	}
	
	public SlaveAutodiscoveryScheduler getSlaveLocalScheduler() {
		return new SlaveAutodiscoveryScheduler(appProperties.getLocalAutodetectionPeriod());
	}
	
	public SlaveAutodiscoveryScheduler getSlaveGlobalScheduler() {
		return new SlaveAutodiscoveryScheduler(appProperties.getGlobalAutodetectionPeriod());
	}
	
	public SlaveGlobalAutodiscoverer getGlobalDiscoverer() {
		return new SlaveGlobalAutodiscoverer(getSlaveGlobalScheduler(), 
											 getGlobalIpFJPScanner(), 
											 getMemberIpMonitor(),
											 appProperties);
	}
	
	public SlaveLocalAutodiscoverer getLocalDiscoverer() {
		return new SlaveLocalAutodiscoverer(getSlaveLocalScheduler(), 
											getLocalIpFJPScanner(), 
											getMemberIpMonitor(),
											appProperties);
	}
	
	//Slave transfer scheduler
	public SlaveScheduler getSlaveTransferScheduler() {
		return new SlaveScheduler(appProperties.getSlaveTransferScheduleInterval());
	}
	
	//Slave repository scheduler
	public SlaveScheduler getSlaveRepositoryScheduler() {
		return new SlaveScheduler(appProperties.getSlaveRepositoryScheduleInterval());
	}
	//============================================================
	
	public void initAsMaster() throws InitializationException {
		//Separate thread intended for (master-side) application shutdown
		masterShutdownThread = new MasterShutdownThread(appProperties);
		masterShutdownThread.start();
		
		//Others
		integerTransformer = new IntegerTransformer();
		protocolStatusMapper = new ProtocolStatusMapper();
		
		//Repository operations
		repositoryVisitor = new RepositoryVisitor(getBaseRepositoryOperations(), appProperties);
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getIntegerTransformer(),
															getLongTransformer(), 
															getBaseRepositoryOperations(),
															appProperties);
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
																  getBaseRepositoryOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getHealthCheckOperation(),
																  getBatchFilesTransferOperation(),
																  appProperties);
		
		transferManagerStateMonitor = new TransferManagerStateMonitor();
		
		repositoryManager = new RepositoryManager(getRepositoryVisitor(), 
															  getBaseRepositoryOperations(), 
															  getTransferManagerStateMonitor(),
															  appProperties);
		
		masterRepositoryScheduler = new MasterRepositoryScheduler(appProperties);
		
		inboundTransferManager = new InboundTransferManager(getHealthCheckOperation(), 
															getFullFileTransferOperation(), 
															getTransferManagerStateMonitor(), 
															appProperties);
	}
	
	public void initAsSlave() throws InitializationException {
		//Others
		integerTransformer = new IntegerTransformer();
		protocolStatusMapper = new ProtocolStatusMapper();
		
		//Repository operations
		repositoryStatusMapper = new RepositoryStatusMapper();
		repositoryVisitor = new RepositoryVisitor(getBaseRepositoryOperations(), appProperties);
		slaveRepositoryManager = new SlaveRepositoryManager(getBaseRepositoryOperations(), 
															getRepositoryStatusMapper());
//		slaveRepositoryManager.init();
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getIntegerTransformer(),
															getLongTransformer(), 
															getBaseRepositoryOperations(),
															appProperties);
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
																  getBaseRepositoryOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getHealthCheckOperation(),
																  getBatchFilesTransferOperation(),
																  appProperties);
		
		//autodiscovering
		ipValidator = new IpValidator(healthCheckOperation, appProperties.getMasterPort(), appProperties.getSocketSoTimeout());
		
		/// autodiscovering ///
		ipAutodiscoverer = new IpAutodiscoverer(getMemberIpMonitor(), 
												getLocalDiscoverer(), 
												getGlobalDiscoverer());
		Thread ipAutodiscovererThread = new Thread(ipAutodiscoverer);
		ipAutodiscovererThread.setName("IpAutodiscovererThread");
		ipAutodiscovererThread.start();
		
		transferManagerStateMonitor = new TransferManagerStateMonitor();
		
		inboundTransferManager = new InboundTransferManager(getHealthCheckOperation(), 
															getFullFileTransferOperation(), 
															getTransferManagerStateMonitor(), 
															appProperties);
		
		outboundTransferManager = new OutboundTransferManager(getMemberIpMonitor(), 
															  getHealthCheckOperation(), 
															  getFullFileTransferOperation(), 
															  getTransferManagerStateMonitor(), 
															  appProperties);
		
		repositoryManager = new RepositoryManager(getRepositoryVisitor(), 
															  getBaseRepositoryOperations(), 
															  getTransferManagerStateMonitor(),
															  appProperties);
	}
	
	public void start(AppProperties appProperties) throws InitializationException {
		this.appProperties = appProperties;
		
		//common
		longTransformer = new LongTransformer();
		filesContextTransformer = new FilesContextTransformer(getLongTransformer());
		baseRepositoryOperations = new BaseRepositoryOperations(getLongTransformer(), 
																getFilesContextTransformer(),
																appProperties);
		
		appInitializer = new AppInitializer(getBaseRepositoryOperations(), appProperties);
		appInitializer.initSysDirectory();	
		
		memberIpMonitor = new MemberIpMonitor(getBaseRepositoryOperations(), appProperties);
		/////////////////////////////////////////////////////////////////////////////////////
		
		MemberType mt = getMemberIpMonitor().memberTypeByMemberId(appProperties.getMemberId());
		if(MemberType.SOURCE == mt) {
			initAsMaster();
			
			Thread inTh = new Thread(getInboundTransferManager());
			inTh.setName(getInboundTransferManager().getClass().getSimpleName());
			inTh.start();
			
			Thread repoTh = new Thread(getRepositoryManager().getScaner());
			repoTh.setName(RepositoryScaner.class.getSimpleName());
			repoTh.start();
		}
		
		if(MemberType.CLOUD == mt) {
			initAsSlave();
			
			Thread inTh = new Thread(getInboundTransferManager());
			Thread outTh = new Thread(getOutboundTransferManager());
			
			inTh.setName(getInboundTransferManager().getClass().getSimpleName());
			outTh.setName(getOutboundTransferManager().getClass().getSimpleName());
			
			inTh.start();
			outTh.start();
			
			Thread repoTh = new Thread(getRepositoryManager().getScaner());
			repoTh.setName(RepositoryScaner.class.getSimpleName());
			repoTh.start();
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
	
	private ProtocolStatusMapper getProtocolStatusMapper() {
		return protocolStatusMapper;
	}
	
	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
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

	public HealthCheckOperation getHealthCheckOperation() {
		return healthCheckOperation;
	}

	public StatusAndHealthCheckOperation getStatusAndHealthCheckOperation() {
		return statusAndHealthCheckOperation;
	}

	public AppInitializer getAppInitializer() {
		return appInitializer;
	}

	public IpValidator getIpValidator() {
		return ipValidator;
	}

	public MemberIpMonitor getMemberIpMonitor() {
		return memberIpMonitor;
	}

	public InboundTransferManager getInboundTransferManager() {
		return inboundTransferManager;
	}

	public OutboundTransferManager getOutboundTransferManager() {
		return outboundTransferManager;
	}

	public TransferManagerStateMonitor getTransferManagerStateMonitor() {
		return transferManagerStateMonitor;
	}
	
}
