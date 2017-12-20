package ipscanner;

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
	
	private BaseRepositoryOperations bro;
	
	// number of ips that a single worker has to try
	private final int workPerThread;
	
	// thread pool size
	private final int fjpSize;
	
	private IpRangesAnalyzer ipRangesAnalyzer;
	
	private int masterPort;
	
	public IpFJPScanner(BaseRepositoryOperations bro, IpRangesAnalyzer ipRangesAnalyzer, int workPerThread, AppProperties appProperties) {
		super();
		this.bro = bro;
		this.ipRangesAnalyzer = ipRangesAnalyzer;
		this.masterPort = appProperties.getMasterPort();
		this.workPerThread = workPerThread;
		this.fjpSize = appProperties.getFjpSize();
	}

	public String scan(String ipRanges) {
		ipRangesAnalyzer.reset(ipRanges);
		
		//Run scan process in fork/join pool
		IpAction ipAction = new IpAction();
		
		ForkJoinPool fjp = new ForkJoinPool(fjpSize);
		
		logger.trace("[" + this.getClass().getSimpleName() + "] start scan for " + ipRanges);
		long start = System.currentTimeMillis();
		fjp.invoke(ipAction);
		long end = System.currentTimeMillis();
		logger.trace("[" + this.getClass().getSimpleName() + "] scan done in " + (end-start)/1000 + " seconds");
		
		fjp.shutdownNow();
		
		//Get the result
		List<String> activeIps = ipAction.getActiveIps();
		String masterIp = null;

		//Analyze the result
		if(activeIps.size() == 1) {
			masterIp = activeIps.get(0);
		}
		if(activeIps.size() > 1) {
			//TODO: throw an exception
			//		Will change in the future. One node will be able to find all of the other nodes in the system
		}
		
		return masterIp;
	}
	
	private class IpAction extends RecursiveAction {

		private static final long serialVersionUID = 1L;
		
		private int ipsChunkId;
		
		//ips to be scan
		private List<String> ips;
		
		//result list of ips where connection was established. 
		//only one master is possible, hence no more that one element must be added.
		private List<String> activeIps;
		
		public IpAction(int ipActionId,
						List<String> ips, 
						List<String> activeIps) {
			super();
			this.ipsChunkId = ipActionId;
			this.ips = ips;
			this.activeIps = activeIps;
		}

		public IpAction() {
			super();
			this.activeIps = new ArrayList<>();
			this.ipsChunkId = 1;
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

					invokeAll(new IpAction(++ipsChunkId, null, activeIps),
							  new IpAction(ipsChunkId, ipsUnitOfWork, activeIps));
					
				}
			} else {
				for(String ip: ips) {
					Socket s = new Socket();
					try {
						logger.trace("[" + this.getClass().getSimpleName() + "] checking " + ip);
						
						s.connect(new InetSocketAddress(ip, masterPort), 1000);
						activeIps.add(ip);
						
						logger.trace("[" + this.getClass().getSimpleName() + "] connecting to " + ip);
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

		public List<String> getActiveIps() {
			return activeIps;
		}
		
	}
	
}
