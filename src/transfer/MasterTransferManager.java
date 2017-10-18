package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import main.AppProperties;
import transfer.constant.MasterSlaveCommunicationStatus;
import transfer.constant.MasterStatus;
import transfer.constant.MasterTransferThreadStatus;
import transfer.constant.ProtocolStatusMapper;

/**
 * Responsible for the full cycle file transfer(from master side). The cycle
 * consists of: (1) health check message (2) metadata message (3) data message
 * (repeats one or more times)
 */
public class MasterTransferManager {

	private final int smallTimeout;
	
	private final int bigTimeout;
	
	private final int socketSoTimeout;
	
	private Logger logger = LogManager.getRootLogger();

	private FullFileTransferOperation ffto;
	
	private StatusTransferOperation sto;
	
	private HealthCheckOperation hco;
	
	private StatusAndHealthCheckOperation shco;

	private MasterSlaveCommunicationPool slaveCommunicationPool;

	private ProtocolStatusMapper statusMapper;

	// Server socket of the master
	private ServerSocket master;

	// Pool of master-client communication threads
	private ExecutorService slaveThreadPool;

	private MasterTransferThread masterTransferThread;

	public MasterTransferManager(AppProperties ap) {
		this.smallTimeout = ap.getSmallPoolingTimeout();
		this.bigTimeout = ap.getBigPoolingTimeout();
		this.socketSoTimeout = ap.getSocketSoTimeout();
	}
	
	public void init(FullFileTransferOperation ffto,
					 StatusTransferOperation sto,
					 HealthCheckOperation hco,
					 StatusAndHealthCheckOperation shco,
					 MasterSlaveCommunicationPool scp,
					 ProtocolStatusMapper sm,
					 AppProperties ap) 
	{
		logger.info("[" + this.getClass().getSimpleName() + "] initialization start");

		this.ffto = ffto;
		this.sto = sto;
		this.hco = hco;
		this.shco = shco;
		this.slaveCommunicationPool = scp;
		this.statusMapper = sm;

		this.masterTransferThread = new MasterTransferThread();
		this.slaveThreadPool = Executors.newCachedThreadPool();

		try {
			this.master = new ServerSocket(ap.getMasterPort());
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e);
		}

		logger.info("[" + this.getClass().getSimpleName() + "] initialization end");
	}

	public void destroy() {
		// stop MasterTransferThread
		// close server socket
		// close thread pool
	}

	/**
	 * Establishes connection with the slave and passes client socket to a
	 * separate thread of execution
	 * @throws IOException 
	 */
	private void acceptSlave() throws IOException {
		Socket slave;
		logger.info("[" + this.getClass().getSimpleName() + "] waiting for slave to connect");
		slave = master.accept();
		slave.setSoTimeout(socketSoTimeout);
		logger.info("[" + this.getClass().getSimpleName() + "] slave connected");

		// Pass os and is to an allocated thread, initial status
		MasterSlaveCommunicationThread communication = new MasterSlaveCommunicationThread(slave);
		slaveThreadPool.execute(communication);
	}

	/**
	 * Adds newly created master slave communication into the pool
	 */
	private void addSlave(MasterSlaveCommunicationThread communication) {
		slaveCommunicationPool.add(communication);
	}
	
	/**
	 * Removes terminated master slave communication from the pool
	 */
	private void removeSlave(MasterSlaveCommunicationThread communication) {
		slaveCommunicationPool.remove(communication);
	}
	
	/**
	 * Requests all running to change their status to BUSY after the last
	 * transfer operation is completed
	 */
	private void pauseSlaves() {
		for (MasterSlaveCommunicationThread communication: slaveCommunicationPool.get()) {
			communication.setRequestedStatus(MasterSlaveCommunicationStatus.BUSY);
		}
	}

	/**
	 * Requests all running to change their status to BUSY after the last
	 * transfer operation is completed
	 */
	private void resumeSlaves() {
		for (MasterSlaveCommunicationThread communication: slaveCommunicationPool.get()) {
			communication.setRequestedStatus(MasterSlaveCommunicationStatus.READY);
		}
	}

	/**
	 * Collects states of all running threads and consolidates them into
	 * MasterTransferManager state. This state can be defined only when states
	 * of all running threads are equal.
	 */
	private MasterTransferThreadStatus statusSlaves() {
		MasterTransferThreadStatus status = MasterTransferThreadStatus.EMPTY;
		
		List<MasterSlaveCommunicationThread> list = slaveCommunicationPool.get();
		if (list.size() > 0) {
			
			MasterSlaveCommunicationStatus baseValue = list.get(0).getActualStatus();
			logger.trace("[" + this.getClass().getSimpleName() + "] MasterSlaveCommunicationStatus of MasterSlaveCommunication[0] "
					+ " is: " + baseValue);
			
			status = statusMapper.map(baseValue);

			if (list.size() > 1) {
				for(int i=1; i<list.size(); i++) {
					MasterSlaveCommunicationThread communication = list.get(i);
					
					logger.trace("[" + this.getClass().getSimpleName() + "] MasterSlaveCommunicationStatus of "
							+ "MasterSlaveCommunication["+ i + "] is: " + communication.getActualStatus());
					
					if (communication.getActualStatus() != baseValue) {
						status = MasterTransferThreadStatus.TRANSIENT;
					}
				}
			}
		} 
		else {
			logger.trace("[" + this.getClass().getSimpleName() + "] Slave communication pool is empty");
		}
		
		return status;
	}

	/**
	 * Entry point to start full transfer operation 
	 * @throws IOException 
	 * @throws WrongOperationException 
	 */
	private void transfer(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		ffto.executeAsMaster(os, is);
	}
	
	/**
	 * Entry point to send status message
	 * @throws IOException 
	 * @throws WrongOperationException 
	 */
	private void transferBusy(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		shco.executeAsMaster(os, is, MasterStatus.BUSY);
	}

	public MasterTransferThread getMasterTransferThread() {
		return masterTransferThread;
	}

	/**
	 * Its main responsibility is to accept incoming connections
	 * pause/resume/status methods are invoked from separate thread(MasterCommunicationProviderThread) 
	 */
	public class MasterTransferThread implements Runnable {

		private MasterTransferThread() {
		}
		
		@Override
		public void run() {
			try {
				logger.info("[" + this.getClass().getSimpleName() + "] start");
				for (;;) {
					acceptSlave();
				}
			} catch (Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}

		/**
		 * Blocks until all communications are not paused  
		 * @throws InterruptedException 
		 */
		public void pause() throws InterruptedException {
			logger.info("[" + this.getClass().getSimpleName() + "] pausing start");
			
			if (status() != MasterTransferThreadStatus.EMPTY) {
				// Connections that come after this invocation are already in BUSY
				pauseSlaves();
				while (status() != MasterTransferThreadStatus.BUSY) {
					
					//Corner case:
					//At the moment when status is going to be invoked all
					//communication threads crash that leads to MaterCommunicationProvider
					//blocking in pause method, until new connection comes.
					//To avoid this exit the method is communication pool is empty
					if(status() == MasterTransferThreadStatus.EMPTY) {
						break;
					}
					
					//Pausing of threads can take much time. Wait until all threads are not paused.
					//Wait 1 minute to avoid resources overconsumption.
					Thread.sleep(bigTimeout);
				}
			}
			
			logger.info("[" + this.getClass().getSimpleName() + "] pausing end");
		}

		/**
		 * Blocks until all communications are not paused 
		 *
		 * resume(), pause() and status() are not invoked simultaneous, only
		 * consequently
		 * @throws InterruptedException 
		 */
		public void resume() throws InterruptedException {
			logger.info("[" + this.getClass().getSimpleName() + "] resuming start");
			
			if (status() != MasterTransferThreadStatus.EMPTY) {
				while (status() != MasterTransferThreadStatus.READY) {
					// Multiple invocations to avoid the following situation:
					// 1. Exists one communication in BUSY
					// 2. status() return BUSY
					// 3. new communication BUSY is ADDED
					// 4. status() return BUSY forever because resumeSlave() doesn't consider new communication
					resumeSlaves();
					
					//Starting of threads can take much time. Wait until all threads are not started.
					//Wait 1 minute to avoid resources overconsumption.
					Thread.sleep(smallTimeout);
				}
			}
			
			logger.info("[" + this.getClass().getSimpleName() + "] resuming end");
		}

		public MasterTransferThreadStatus status() {
			return statusSlaves();
		}

	}

	public class MasterSlaveCommunicationThread implements Runnable {

		private Socket slave;

		private OutputStream os;

		private InputStream is;

		// Status to be set. Requested by MasterCommunicationProvider.
		private MasterSlaveCommunicationStatus requestedStatus;

		// Actual status of the thread. It's impossible to set immediately
		// actualStatus to requestedStatus.
		private MasterSlaveCommunicationStatus actualStatus;

		private MasterSlaveCommunicationThread(Socket slave) {
			super();

			logger.info("[" + this.getClass().getSimpleName() + "] initialization start");

			this.slave = slave;
			this.requestedStatus = MasterSlaveCommunicationStatus.BUSY;
			this.actualStatus = MasterSlaveCommunicationStatus.BUSY;

			try {
				this.os = slave.getOutputStream();
				this.is = slave.getInputStream();
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e);
			}

			logger.info("[" + this.getClass().getSimpleName() + "] initialization end");
		}

		@Override
		public void run() {
			try {
				
				addSlave(this);
				
				for (;;) {
					if (actualStatus == MasterSlaveCommunicationStatus.READY) {
						logger.info("[" + this.getClass().getSimpleName() + "] transfer start");
						transfer(os, is);
						logger.info("[" + this.getClass().getSimpleName() + "] transfer end");
					} else if (actualStatus == MasterSlaveCommunicationStatus.BUSY) {
						logger.info("[" + this.getClass().getSimpleName() + "] BUSY start");
						transferBusy(os, is);
						logger.info("[" + this.getClass().getSimpleName() + "] BUSY end");
					}

					// change status
					if (requestedStatus == MasterSlaveCommunicationStatus.BUSY) {
						actualStatus = MasterSlaveCommunicationStatus.BUSY;
					} else if (requestedStatus == MasterSlaveCommunicationStatus.READY) {
						actualStatus = MasterSlaveCommunicationStatus.READY;
					}

					//One iteration of the loop in a minute is sufficient for thread status check.
					//Wait 1 minute to avoid resources overconsumption.
					Thread.sleep(bigTimeout);
				}
			} catch (Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] ", e);
			}
			finally {
				try {
					
					removeSlave(this);
					
					os.close();
					is.close();
					slave.close();
					logger.info("[" + this.getClass().getSimpleName() + "] [MasterSlaveCommunicationThread] connection is closed");
				} 
				catch (IOException e) {
					logger.error("[" + this.getClass().getSimpleName() + "] can't close io-streams / socket", e);
				}
			}
		}
		
		public MasterSlaveCommunicationStatus getActualStatus() {
			return actualStatus;
		}

		public void setRequestedStatus(MasterSlaveCommunicationStatus requestedStatus) {
			this.requestedStatus = requestedStatus;
		}

	}

}
