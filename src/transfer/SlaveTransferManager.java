package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.SlaveAutodiscoveryAdapter;
import exception.BatchFileTransferException;
import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import scheduler.SlaveTransferScheduler;
import transfer.constant.MasterStatus;

public class SlaveTransferManager {
	
	private final int bigTimeout;
	
	private final int socketSoTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private SlaveTransferScheduler scheduler;
	
	private FullFileTransferOperation ffto;
	
	private StatusTransferOperation sto;
	
	private HealthCheckOperation hco;
	
	private SlaveAutodiscoveryAdapter saa;
	
	private AppProperties ap;
	
	public SlaveTransferManager(AppProperties ap) {
		this.bigTimeout = ap.getBigPoolingTimeout();
		this.socketSoTimeout = ap.getSocketSoTimeout();
	}
	
	public void init(FullFileTransferOperation ffto,
					 StatusTransferOperation sto,
					 HealthCheckOperation hco,
					 SlaveTransferScheduler sts,
					 SlaveAutodiscoveryAdapter saa,
					 AppProperties ap) 
	{
		this.scheduler = sts;
		this.ffto = ffto;
		this.sto = sto;
		this.hco = hco;
		this.saa = saa;
		this.ap = ap;
	}

	public void destroy() {
		//wait/stop SlaveMasterCommunicationThread
		//stop SlaveTransferThread
	}
	
	private Thread connect(String masterIp, int masterPort) throws UnknownHostException, IOException {
		Socket master = null;
		
		logger.info("[" + this.getClass().getSimpleName() + "] opening socket to " + masterIp + ":" + masterPort);
		master = new Socket(masterIp, masterPort);
		master.setSoTimeout(socketSoTimeout);
		logger.info("[" + this.getClass().getSimpleName() + "]  socket to " + masterIp + ":" + masterPort + " opened");
		
		Thread thread = new Thread(new SlaveMasterCommunicationThread(master));
		thread.setName("SlaveTransferThread");
		thread.start();
		
		// return thread that represents SlaveMasterCommunicationThread in order to join on it
		return thread;
	}
	
	private void transfer(OutputStream os, InputStream is) throws InterruptedException, IOException, MasterNotReadyDuringBatchTransfer, WrongOperationException, BatchFileTransferException {
		//healthcheck returns MATER status
		MasterStatus status = hco.executeAsSlave(os, is);
		if(status == MasterStatus.READY && scheduler.isScheduled()) {
			
			//MASTER status request grabs transfer operation on MASTER
			//if READY is returned MASTER is waiting to start transfer request
			//however it plausible that between healthcheck and status check operation
			//MASTER changes its status from READY TO BUSY
			status = sto.executeAsSlave(os, is);
			if(status == MasterStatus.READY) {
				ffto.executeAsSlave(os, is);
				scheduler.scheduleNext();
			}
			
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
			saa.startup();
			
			String masterIp = saa.getMasterIp();
			int masterPort = ap.getMasterPort();
			
			for(;;) {
				try {
					Thread t = connect(masterIp, masterPort);
					t.join();
				} catch (Exception e) {
					logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
					
					saa.failure();
					masterIp = saa.getMasterIp();
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
