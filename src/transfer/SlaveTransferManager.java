package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import scheduler.SlaveTransferScheduler;
import transfer.constant.MasterStatus;

public class SlaveTransferManager {
	
	private final int bigTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private SlaveTransferScheduler scheduler;
	
	private FullFileTransferOperation ffto;
	
	private StatusTransferOperation sto;
	
	private AppProperties ap;
	
	public SlaveTransferManager(AppProperties ap) {
		this.bigTimeout = ap.getBigPoolingTimeout();
	}
	
	public void init(FullFileTransferOperation ffto,
					 StatusTransferOperation sto,
					 SlaveTransferScheduler sts,
					 AppProperties ap) 
	{
		this.scheduler = sts;
		this.ffto = ffto;
		this.sto = sto;
		this.ap = ap;
	}

	public void destroy() {
		//wait/stop SlaveMasterCommunicationThread
		//stop SlaveTransferThread
	}
	
	private Thread connect() throws UnknownHostException, IOException {
		Socket master = null;
		String ip = ap.getMasterIp();
		int port = ap.getMasterPort();
		
		logger.info("[" + this.getClass().getSimpleName() + "] opening socket to " + ip + ":" + port);
		master = new Socket(ip, port);
		logger.info("[" + this.getClass().getSimpleName() + "]  socket to " + ip + ":" + port + " opened");
		
		Thread thread = new Thread(new SlaveMasterCommunicationThread(master));
		thread.setName("SlaveTransferThread");
		thread.start();
		
		// return thread that represents SlaveMasterCommunicationThread in order to join on it
		return thread;
	}
	
	private void transfer(OutputStream os, InputStream is) throws InterruptedException, IOException, MasterNotReadyDuringBatchTransfer, WrongOperationException {
		//get status request(also used as health check)
		MasterStatus status = sto.executeAsSlave(os, is);
		if(status == MasterStatus.READY && scheduler.isScheduled()) {
			ffto.executeAsSlave(os, is);
		}
	}

	public Thread getSlaveTransferThread() {
		logger.info("[" + this.getClass().getSimpleName() + "] initialization of SlaveTransferThread start");
		
		Thread thread = new Thread(new SlaveTransferThread());
		thread.setName("SlaveTransferThread");
		
		logger.info("[" + this.getClass().getSimpleName() + "] initialization SlaveTransferThread end");

		return thread;
	}
	
	private class SlaveTransferThread implements Runnable {

		@Override
		public void run() {
			for(;;) {
				try {
					Thread t = connect();
					t.join();
				} catch (Exception e) {
					logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
				}
				//After slave master communication is broken try to reconnect
			}
		}
		
	}
	
	private class SlaveMasterCommunicationThread implements Runnable {

		private Socket master;

		private OutputStream os;

		private InputStream is;

		public SlaveMasterCommunicationThread(Socket master) {
			super();

			this.master = master;

			try {
				this.os = master.getOutputStream();
				this.is = master.getInputStream();
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e);
			}
		}

		@Override
		public void run() {
			try {
				for(;;) {
					transfer(os, is);
					
					//Determines frequency of health check.
					//Wait 1 minute to avoid resources overconsumption.
					Thread.sleep(bigTimeout);
				}
			} catch (Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
			finally {
				try {
					os.close();
					is.close();
					master.close();
				} catch (IOException e) {
					logger.error("[" + this.getClass().getSimpleName() + "] can't close io-streams / socket", e);
				}
				
			}
		}

	}

}
