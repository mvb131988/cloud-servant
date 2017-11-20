package ipscanner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;

public class IpScanner {

	private Logger logger = LogManager.getRootLogger();
	
	private IpRangeAnalyzer ipRangeAnalyzer;
	
	private AppProperties appProperties;
	
	private int masterPort; 
	
	public IpScanner(IpRangeAnalyzer ipRangeAnalyzer, AppProperties appProperties) {
		this.ipRangeAnalyzer = ipRangeAnalyzer;
		this.appProperties = appProperties;
		this.masterPort = this.appProperties.getMasterPort();
	}
	
	//Scans until not find first node(master or slave)
	//TODO: rename in findFirst. Add verification that found node is master
	public String scan() {
		String nextIp = null;
		
		ipRangeAnalyzer.reset();
		
		while (ipRangeAnalyzer.hasNext()) {
			nextIp = ipRangeAnalyzer.next();
			System.out.println(nextIp);
			
			try {
				Caller c = new Caller(nextIp); 
				Thread t = new Thread(c);
				t.start();
				Thread.sleep(100);
				c.close();
				t.join();
				
				if(c.getResult() != null) {
					return c.getResult();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public class Caller implements Runnable{

		private String ip;
		
		private Socket s;
		
		private String result;
		
		public Caller(String ip) {
			this.ip = ip;
		}
		
		@Override
		public void run() {
			try {
				logger.trace("[" + this.getClass().getSimpleName() + "] checking " + ip);
				
				s = new Socket();
				s.setSoTimeout(10);
				s.connect(new InetSocketAddress(ip, masterPort));
				result = ip;
				
				logger.trace("[" + this.getClass().getSimpleName() + "] connecting to " + ip);
			} catch (UnknownHostException e) {
				logger.trace("[" + this.getClass().getSimpleName() + "] fail to connect to " + ip);
			} catch (IOException e) {
				logger.trace("[" + this.getClass().getSimpleName() + "] fail to connect to " + ip);
			}
		}
		
		public void close() {
			try {
				Thread.sleep(10);
				s.close();
			} catch (IOException | InterruptedException e) {
//				e.printStackTrace();
			}
		}

		public String getResult() {
			return result;
		}
	}
	
}
