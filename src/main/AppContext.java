package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.CloudMemberAutodiscoverer;
import autodiscovery.IpAutodiscoverer;
import autodiscovery.MemberAutodiscoveryScheduler;
import autodiscovery.MemberIpMonitor;
import autodiscovery.MemberType;
import autodiscovery.SourceMemberAutodiscoverer;
import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpRangeAnalyzer;
import autodiscovery.ipscanner.IpRangesAnalyzer;
import autodiscovery.ipscanner.IpValidator;
import exception.InitializationException;
import repository.AsynchronySearcherManager;
import repository.BaseRepositoryOperations;
import repository.RepositoryConsistencyChecker;
import repository.RepositoryManager;
import repository.RepositoryManager.RepositoryScaner;
import repository.RepositoryVisitor;
import repository.status.RepositoryStatusMapper;
import transfer.BaseTransferOperations;
import transfer.BatchFilesTransferOperation;
import transfer.FileTransferOperation;
import transfer.FullFileTransferOperation;
import transfer.HealthCheckOperation;
import transfer.InboundTransferManager;
import transfer.OutboundTransferManager;
import transfer.StatusTransferOperation;
import transfer.TransferManagerStateMonitor;
import transformer.FilesContextTransformer;
import transformer.IntegerTransformer;
import transformer.LongTransformer;

public class AppContext {

	private Logger logger = LogManager.getRootLogger();

	private AppProperties appProperties;
	
	private AsynchronySearcherManager asynchronySearcherManager;
	
	private RepositoryStatusMapper repositoryStatusMapper;
	
	private StatusTransferOperation statusTransferOperation;
	
	private HealthCheckOperation healthCheckOperation;
	
	private BatchFilesTransferOperation batchTransferOperation;
	
	private FullFileTransferOperation fullFileTransferOperation;
	
	private FileTransferOperation fileTransferOperation;
	
	private RepositoryVisitor repositoryVisitor;
	
	private RepositoryManager repositoryManager;
	
	private BaseRepositoryOperations baseRepositoryOperations;
	
	private BaseTransferOperations baseTransferOperations;
	
	private FilesContextTransformer filesContextTransformer;
	
	private IntegerTransformer integerTransformer;
	
	private LongTransformer longTransformer;
	
	private MasterShutdownThread masterShutdownThread;
	
	private AppInitializer appInitializer;
	
	private IpValidator ipValidator;
	
	private IpAutodiscoverer ipAutodiscoverer;
	
	private MemberIpMonitor memberIpMonitor;
	
	private InboundTransferManager inboundTransferManager;
	
	private OutboundTransferManager outboundTransferManager;
	
	private TransferManagerStateMonitor transferManagerStateMonitor;
	
	private RepositoryConsistencyChecker repositoryConsistencyChecker;
	
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
	
	public MemberAutodiscoveryScheduler getSlaveLocalScheduler() {
		return new MemberAutodiscoveryScheduler(appProperties.getLocalAutodetectionPeriod());
	}
	
	public MemberAutodiscoveryScheduler getSlaveGlobalScheduler() {
		return new MemberAutodiscoveryScheduler(appProperties.getGlobalAutodetectionPeriod());
	}
	
	public CloudMemberAutodiscoverer getGlobalDiscoverer() {
		return new CloudMemberAutodiscoverer(getSlaveGlobalScheduler(), 
											 getGlobalIpFJPScanner(), 
											 getMemberIpMonitor(),
											 appProperties);
	}
	
	public SourceMemberAutodiscoverer getLocalDiscoverer() {
		return new SourceMemberAutodiscoverer(getSlaveLocalScheduler(), 
											getLocalIpFJPScanner(), 
											getMemberIpMonitor(),
											appProperties);
	}

	//============================================================
	
	public void initAsMaster() throws InitializationException {
		//Separate thread intended for (master-side) application shutdown
		masterShutdownThread = new MasterShutdownThread(appProperties);
		masterShutdownThread.start();
		
		//Others
		integerTransformer = new IntegerTransformer();
		
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
		batchTransferOperation = new BatchFilesTransferOperation(getBaseTransferOperations(),
																 getFileTransferOperation(),
																 getStatusTransferOperation(),
																 getFilesContextTransformer(),
																 null,
																 null,
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
		
		inboundTransferManager = new InboundTransferManager(getHealthCheckOperation(), 
															getFullFileTransferOperation(), 
															getTransferManagerStateMonitor(), 
															appProperties);
	}
	
	public void initAsSlave() throws InitializationException {
		//Others
		integerTransformer = new IntegerTransformer();
		
		//Repository operations
		repositoryStatusMapper = new RepositoryStatusMapper();
		repositoryVisitor = new RepositoryVisitor(getBaseRepositoryOperations(), appProperties);
		
		//Transfer operations
		baseTransferOperations = new BaseTransferOperations(getIntegerTransformer(),
															getLongTransformer(), 
															getBaseRepositoryOperations(),
															appProperties);
		fileTransferOperation = new FileTransferOperation(getBaseTransferOperations(), 
														  getBaseRepositoryOperations(),
														  appProperties);
		
		asynchronySearcherManager = new AsynchronySearcherManager(getBaseRepositoryOperations(), 
			      												  getFilesContextTransformer(), 
			      												  getRepositoryStatusMapper(), 
			      												  appProperties);
		repositoryConsistencyChecker = 
				new RepositoryConsistencyChecker(getBaseRepositoryOperations(), 
												 getLongTransformer(), 
												 getFilesContextTransformer());
		
		statusTransferOperation = new StatusTransferOperation(getBaseTransferOperations());
		healthCheckOperation = new HealthCheckOperation(getBaseTransferOperations());
		batchTransferOperation = new BatchFilesTransferOperation(getBaseTransferOperations(),
				 												 getFileTransferOperation(),
				 												 getStatusTransferOperation(),
				 												 getFilesContextTransformer(),
				 												 getAsynchronySearcherManager(),
				 												 getRepositoryConsistencyChecker(),
				 												 appProperties);
		fullFileTransferOperation = new FullFileTransferOperation(getBaseTransferOperations(),
																  getBaseRepositoryOperations(),
																  getFileTransferOperation(),
																  getStatusTransferOperation(),
																  getHealthCheckOperation(),
																  getBatchFilesTransferOperation(),
																  appProperties);
		
		//autodiscovering
		ipValidator = new IpValidator(healthCheckOperation, 
									  appProperties.getMasterPort(), 
									  appProperties.getSocketSoTimeout());
		
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
	
	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	public RepositoryStatusMapper getRepositoryStatusMapper() {
		return repositoryStatusMapper;
	}

	public HealthCheckOperation getHealthCheckOperation() {
		return healthCheckOperation;
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

	public AsynchronySearcherManager getAsynchronySearcherManager() {
		return asynchronySearcherManager;
	}

	public RepositoryConsistencyChecker getRepositoryConsistencyChecker() {
		return repositoryConsistencyChecker;
	}
	
}
