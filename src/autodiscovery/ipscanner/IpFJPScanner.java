package autodiscovery.ipscanner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;
import repository.BaseRepositoryOperations;

public class IpFJPScanner {
	
	private static Logger logger = LogManager.getRootLogger();
	
	private static Logger autodiscoveryLogger = LogManager.getLogger("AutodiscoveryLogger");
	
	private BaseRepositoryOperations bro;
	
	private IpValidator ipValidator;
	
	// number of ips that a single worker has to try
	private final int workPerThread;
	
	// thread pool size
	private final int fjpSize;
	
	private IpRangesAnalyzer ipRangesAnalyzer;
	
	private int transferPort;
	
	private final int socketSoTimeout;
	
	public IpFJPScanner(BaseRepositoryOperations bro, 
						IpValidator ipValidator, 
						IpRangesAnalyzer ipRangesAnalyzer, 
						int workPerThread, 
						AppProperties appProperties) {
		super();
		this.bro = bro;
		this.ipValidator = ipValidator;
		this.ipRangesAnalyzer = ipRangesAnalyzer;
		this.transferPort = appProperties.getTransferPort();
		this.workPerThread = workPerThread;
		this.fjpSize = appProperties.getFjpSize();
		this.socketSoTimeout = appProperties.getSocketSoTimeout();
	}

	public List<IpScannerResult> scan(String ipRanges) {
		ipRangesAnalyzer.reset(ipRanges);
		
		//Run scan process in fork/join pool
		IpAction ipAction = new IpAction(ipValidator);
		
		ForkJoinPool fjp = new ForkJoinPool(fjpSize);
		
		autodiscoveryLogger.trace("[" + this.getClass().getSimpleName() + "] start scan for " + ipRanges);
		long start = System.currentTimeMillis();
		fjp.invoke(ipAction);
		long end = System.currentTimeMillis();
		autodiscoveryLogger.trace("[" + this.getClass().getSimpleName() + "] scan done in " + (end-start)/1000 + " seconds");
		
		fjp.shutdownNow();
		
		//Get the result. More than 1 active ips possible for different reasons:
		// - multiple cloud-servant nodes found
		// - non cloud-servant nodes, run on the same as cloud-servant node port 
		List<IpScannerResult> activeIps = ipAction.getActiveIps();
		return activeIps;
	}
	
	private class IpAction extends RecursiveAction {

		private static final long serialVersionUID = 1L;
		
		private int ipsChunkId;
		
		private IpValidator ipValidator;
		
		//ips to be scan
		private List<String> ips;
		
		//result list of ips where connection was established. 
		private List<IpScannerResult> activeIps;
		
		public IpAction(IpValidator ipValidator) {
			super();
			this.activeIps = new ArrayList<>();
			this.ipsChunkId = 1;
			this.ipValidator = ipValidator;
		}
		
		public IpAction(int ipActionId,
						List<String> ips, 
						List<IpScannerResult> activeIps,
						IpValidator ipValidator) {
			super();
			this.ipsChunkId = ipActionId;
			this.ips = ips;
			this.activeIps = activeIps;
			this.ipValidator = ipValidator;
		}

		@Override
		protected void compute() {
			if(ips == null) {
				List<String> ipsUnitOfWork = new ArrayList<>();
				if (ipRangesAnalyzer.hasNext()) {
					
					//create unit of work that could be consumed by a worker thread
					//1. create file chunk[index]
					//BaseRepositoryOperations.IpsChunkWriter writer = bro.getIpsChunkWriter(ipsChunkId);
					for (int i = 0; i < workPerThread; i++) {
						if (!ipRangesAnalyzer.hasNext()) {
							break;
						}
						String ip = ipRangesAnalyzer.next();
						ipsUnitOfWork.add(ip);
						//2. write in chunk[index]
						//writer.write(ip);
						
					}
					//3. close chunk[index]
					//writer.close();

					invokeAll(new IpAction(++ipsChunkId, null, activeIps, ipValidator),
							  new IpAction(ipsChunkId, ipsUnitOfWork, activeIps, ipValidator));
					
				}
			} else {
				for(String ip: ips) {
					Socket s = new Socket();
					try {
						autodiscoveryLogger.trace("[" + this.getClass().getSimpleName() + "] checking " + ip);
						
						s.setSoTimeout(socketSoTimeout);
						s.connect(new InetSocketAddress(ip, transferPort), 1000);
						IpValidatorResult result = ipValidator.isValid(s, ip);
						
						if(result.isResult()) {
							activeIps.add(new IpScannerResult(ip, result.getMemberId()));
							autodiscoveryLogger.trace("[" + this.getClass().getSimpleName() + "] connected to " + ip);
						}
					} catch (UnknownHostException e) {
						//Don't log, prevent too many expected exceptions to be logged  
					} catch (IOException e) {
						//Don't log, prevent too many expected exceptions to be logged  
					}
					finally {
						try {
							s.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		public List<IpScannerResult> getActiveIps() {
			return activeIps;
		}
		
	}
	
}
