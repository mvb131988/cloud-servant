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
	private final static int WORK_PER_THREAD = 2;
	
	private IpRangesAnalyzer ipRangesAnalyzer;
	
	private int masterPort;
	
	public IpFJPScanner(IpRangesAnalyzer ipRangesAnalyzer, AppProperties appProperties) {
		super();
		this.ipRangesAnalyzer = ipRangesAnalyzer;
		this.masterPort = appProperties.getMasterPort();
	}

	public String scan(String ipRanges) {
		ipRangesAnalyzer.reset(ipRanges);
		
		//Run scan process in fork/join pool
		IpAction ipAction = new IpAction(ipRangesAnalyzer, masterPort);
		ForkJoinPool fjp = new ForkJoinPool(4);
		
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
			// throw an exception
		}
		
		return masterIp;
	}
	
	private static class IpAction extends RecursiveAction {

		private static final long serialVersionUID = 1L;

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
						List<String> activeIps) {
			super();
			this.ipRangesAnalyzer = ipRangesAnalyzer;
			this.ips = ips;
			this.activeIps = new ArrayList<>();
			this.masterPort = masterPort;
		}

		public IpAction(IpRangesAnalyzer ipRangesAnalyzer, int masterPort) {
			super();
			this.ipRangesAnalyzer = ipRangesAnalyzer;
			this.activeIps = new ArrayList<>();
			this.masterPort = masterPort;
		}

		@Override
		protected void compute() {
			if(ips == null) {
				List<String> ipsUnitOfWork = new ArrayList<>();
				if (ipRangesAnalyzer.hasNext()) {
					
					//create unit of work that could be consumed by a worker thread
					for (int i = 0; i < WORK_PER_THREAD; i++) {
						if (!ipRangesAnalyzer.hasNext()) {
							break;
						}
						ipsUnitOfWork.add(ipRangesAnalyzer.next());
					}

					invokeAll(new IpAction(ipRangesAnalyzer, null, masterPort, activeIps),
							  new IpAction(ipRangesAnalyzer, ipsUnitOfWork, masterPort, activeIps));
					
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
						//logger.trace("[" + this.getClass().getSimpleName() + "] fail to connect to " + ip);
					} catch (IOException e) {
						//logger.trace("[" + this.getClass().getSimpleName() + "] fail to connect to " + ip);
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
