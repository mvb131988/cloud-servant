package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
public class MemberShutdownThread implements Runnable {

	private Logger logger = LogManager.getRootLogger();
	
	private int adminPort;
	
	public MemberShutdownThread(AppProperties appProperties) {
		adminPort = appProperties.getAdminPort();
	}
	
	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(adminPort);
			Socket s = ss.accept();

			//TODO[FUTURE]: Shutdown
			logger.info("[" + this.getClass().getSimpleName() + "] Shutdown start");
			
			//TODO[FUTURE]: Send acknowledgement
			s.close();
			ss.close();
			
			logger.info("[" + this.getClass().getSimpleName() + "] Shutdown end");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		Thread thread = new Thread(this);
		thread.setName(this.getClass().getSimpleName());
		thread.start();
	}

}
