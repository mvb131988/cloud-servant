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

public class IpFJPScanner {
	
	private static Logger logger = LogManager.getRootLogger();
	
	// number of ips that a single worker has to try
	private final int workPerThread;
	
	// thread pool size
	private final int fjpSize;
	
	private IpRangesAnalyzer ipRangesAnalyzer;
	
	private int masterPort;
	
	public IpFJPScanner(IpRangesAnalyzer ipRangesAnalyzer, int workPerThread, AppProperties appProperties) {
		super();
		this.ipRangesAnalyzer = ipRangesAnalyzer;
		this.masterPort = appProperties.getMasterPort();
		this.workPerThread = workPerThread;
		this.fjpSize = appProperties.getFjpSize();
	}

	public String scan(String ipRanges) {
		ipRangesAnalyzer.reset(ipRanges);
		
		//Run scan process in fork/join pool
		IpAction ipAction = new IpAction(ipRangesAnalyzer, masterPort, workPerThread);
		
		ForkJoinPool fjp = new ForkJoinPool(fjpSize);
		
		logger.trace("[" + this.getClass().getSimpleName() + "] start scan for " + ipRanges);
		long start = System.currentTimeMillis();
		fjp.invoke(ipAction);
		long end = System.currentTimeMillis();
		logger.trace("[" + this.getClass().getSimpleName() + "] scan done in " + (end-start)/1000 + " seconds");
		
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
	
	private static class IpAction extends RecursiveAction {

		private static final long serialVersionUID = 1L;
		
		private final int workPerThread;

		private int masterPort;
		
		private IpRangesAnalyzer ipRangesAnalyzer;
		
		//ips to be scan
		private List<String> ips;
		
		//result list of ips where connection was established. 
		//only one master is possible, hence no more that one element must be added.
		private List<String> activeIps;
		
		public IpAction(IpRangesAnalyzer ipRangesAnalyzer, 
						List<String> ips, 
						int masterPort, 
						List<String> activeIps,
						int workPerThread) {
			super();
			this.ipRangesAnalyzer = ipRangesAnalyzer;
			this.ips = ips;
			this.activeIps = activeIps;
			this.masterPort = masterPort;
			this.workPerThread = workPerThread;
		}

		public IpAction(IpRangesAnalyzer ipRangesAnalyzer, int masterPort, int workPerThread) {
			super();
			this.ipRangesAnalyzer = ipRangesAnalyzer;
			this.activeIps = new ArrayList<>();
			this.masterPort = masterPort;
			this.workPerThread = workPerThread;
		}

		@Override
		protected void compute() {
			if(ips == null) {
				List<String> ipsUnitOfWork = new ArrayList<>();
				if (ipRangesAnalyzer.hasNext()) {
					
					//create unit of work that could be consumed by a worker thread
					for (int i = 0; i < workPerThread; i++) {
						if (!ipRangesAnalyzer.hasNext()) {
							break;
						}
						ipsUnitOfWork.add(ipRangesAnalyzer.next());
					}

					invokeAll(new IpAction(ipRangesAnalyzer, null, masterPort, activeIps, workPerThread),
							  new IpAction(ipRangesAnalyzer, ipsUnitOfWork, masterPort, activeIps, workPerThread));
					
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
