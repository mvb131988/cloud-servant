package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.SlaveAutodiscoveryAdapter;
import exception.BatchFileTransferException;
import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import ipscanner.IpValidator;
import main.AppProperties;
import scheduler.SlaveScheduler;
import transfer.constant.MasterStatus;
import transfer.constant.SlaveMasterCommunicationStatus;

public class SlaveTransferManager {
	
	private final int bigTimeout;
	
	private final int socketSoTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private SlaveScheduler scheduler;
	
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
					 SlaveScheduler sts,
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
	
	private SlaveMasterCommunicationThread connect(String masterIp, int masterPort) throws UnknownHostException, IOException {
		Socket master = null;
		
		logger.info("[" + this.getClass().getSimpleName() + "] opening socket to " + masterIp + ":" + masterPort);
		master = new Socket(masterIp, masterPort);
		master.setSoTimeout(socketSoTimeout);
		logger.info("[" + this.getClass().getSimpleName() + "]  socket to " + masterIp + ":" + masterPort + " opened");
		
		SlaveMasterCommunicationThread slaveMasterCommunicationThread = new SlaveMasterCommunicationThread(master);
		return slaveMasterCommunicationThread;
	}
	
	private void transfer(OutputStream os, InputStream is) throws InterruptedException, IOException, MasterNotReadyDuringBatchTransfer, WrongOperationException, BatchFileTransferException {
		//healthcheck returns MASTER status
		MasterStatus status = hco.executeAsSlave(os, is).getMasterStatus();
		if(status == MasterStatus.READY && scheduler.isScheduled()) {
			
			//MASTER status request grabs transfer operation on MASTER
			//if READY is returned MASTER is waiting to start transfer request
			//however it plausible that between healthcheck and status check operation
			//MASTER changes its status from READY TO BUSY
			status = sto.executeAsSlave(os, is).getMasterStatus();
			if(status == MasterStatus.READY) {
				ffto.executeAsSlave(os, is);
				scheduler.scheduleNext();
			}
			
		}
	}
	
	private void transferBusy(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		//healthcheck returns MASTER status
		MasterStatus status = hco.executeAsSlave(os, is).getMasterStatus();
		logger.info("[" + this.getClass().getSimpleName() + "] master status: " + status);
	}

	//TODO: review the method
	public SlaveTransferThread getSlaveTransferThread() {
		logger.info("[" + this.getClass().getSimpleName() + "] initialization of SlaveTransferThread start");
		
		Thread thread = new Thread(new SlaveTransferThread());
		thread.setName("SlaveTransferThread");
		
		logger.info("[" + this.getClass().getSimpleName() + "] initialization SlaveTransferThread end");

		return new SlaveTransferThread();
	}
	
	public class SlaveTransferThread implements Runnable {

		private Thread communicationThread;
		
		private SlaveMasterCommunicationThread slaveMasterCommunicationThread;
		
		// counts consequent number of failures
		private int failureCounter = 0;
		
		@Override
		public void run() {
			List<String> masterIps = saa.startup(failureCounter);
			
			//At this point only one master ip is expected
			if(masterIps.size() > 1) {
			  String errorMessage = "Only one masterIp is expected, found: " + masterIps.size();
			  String ips = masterIps.stream().collect(Collectors.joining(","));  
			  throw new RuntimeException(errorMessage + " { " + ips + " } ");
			}
			
			String masterIp = masterIps.get(0);
		  
			int masterPort = ap.getMasterPort();

			for(;;) {
				try {
					slaveMasterCommunicationThread = connect(masterIp, masterPort);
					communicationThread = new Thread(slaveMasterCommunicationThread);
					communicationThread.setName("SlaveTransferThread");
					communicationThread.start();
					
					// if connection with master established reset failureCounter
					failureCounter = 0;
					
					communicationThread.join();
					slaveMasterCommunicationThread = null;
				
				} catch (Exception e) {
					logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
					
					slaveMasterCommunicationThread = null;
					masterIps = saa.failure(++failureCounter);
					masterIp = masterIps.get(0);
					
					//TODO: Put timeout here(protection for case when network fails)
					try {
						Thread.sleep(bigTimeout);
					} catch (InterruptedException e1) {
						//TODO: LOG exception
					}
				}
				//After slave master communication is broken try to reconnect
			}
		}
		
		/**
		 * Blocks until communication thread is not transferred in BUSY state or is not terminated
		 * @throws InterruptedException 
		 */
		public void pause() throws InterruptedException {
			logger.info("[" + this.getClass().getSimpleName() + "] pausing start");

			while (slaveMasterCommunicationThread != null &&
				   slaveMasterCommunicationThread.getActualStatus() != SlaveMasterCommunicationStatus.BUSY) {

				slaveMasterCommunicationThread.setRequestedStatus(SlaveMasterCommunicationStatus.BUSY);
				
				// Pausing of threads can take much time. Wait until all threads are not paused.
				// Wait 1 minute to avoid resources overconsumption.
				Thread.sleep(bigTimeout);
			}
			
			logger.info("[" + this.getClass().getSimpleName() + "] pausing end");
		}
		
		/**
		 * Blocks until communication thread is not transferred in READY state or is not terminated
		 * @throws InterruptedException
		 */
		public void resume() throws InterruptedException {
			logger.info("[" + this.getClass().getSimpleName() + "] resuming start");
			
			while (slaveMasterCommunicationThread != null &&
				   slaveMasterCommunicationThread.getActualStatus() != SlaveMasterCommunicationStatus.READY) {
				
				//Even if after requested status set to READY and current SlaveMasterCommunicationThread fails
				//and new created SlaveMasterCommunicationThread has status BUSY, continuous invocation of SlaveMasterCommunicationThread
				//set request status method guarantees that for newly created SlaveMasterCommunicationThread
				//status eventually will be set to READY(hence while terminates)
				slaveMasterCommunicationThread.setRequestedStatus(SlaveMasterCommunicationStatus.READY);
				
				// Pausing of threads can take much time. Wait until all threads are not paused.
				// Wait 1 minute to avoid resources overconsumption.
				Thread.sleep(bigTimeout);
			}
			
			logger.info("[" + this.getClass().getSimpleName() + "] resuming end");
		}
		
	}
	
	private class SlaveMasterCommunicationThread implements Runnable {

		private Socket master;

		private OutputStream os;

		private InputStream is;
		
		private SlaveMasterCommunicationStatus requestedStatus;

		private SlaveMasterCommunicationStatus actualStatus;

		public SlaveMasterCommunicationThread(Socket master) {
			super();

			this.master = master;
			this.requestedStatus = SlaveMasterCommunicationStatus.BUSY;
			this.actualStatus = SlaveMasterCommunicationStatus.BUSY;

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
					if (actualStatus == SlaveMasterCommunicationStatus.READY) {
						logger.info("[" + this.getClass().getSimpleName() + "] transfer start");
						transfer(os, is);
						logger.info("[" + this.getClass().getSimpleName() + "] transfer end");
					} else if (actualStatus == SlaveMasterCommunicationStatus.BUSY) {
						logger.info("[" + this.getClass().getSimpleName() + "] BUSY start");
						transferBusy(os, is);
						logger.info("[" + this.getClass().getSimpleName() + "] BUSY end");
					}

					// change status
					if (requestedStatus == SlaveMasterCommunicationStatus.BUSY) {
						actualStatus = SlaveMasterCommunicationStatus.BUSY;
					} else if (requestedStatus == SlaveMasterCommunicationStatus.READY) {
						actualStatus = SlaveMasterCommunicationStatus.READY;
					}

					//One iteration of the loop in a minute is sufficient for thread status check.
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
		
		public SlaveMasterCommunicationStatus getActualStatus() {
			return actualStatus;
		}

		public void setRequestedStatus(SlaveMasterCommunicationStatus requestedStatus) {
			this.requestedStatus = requestedStatus;
		}

	}

}
