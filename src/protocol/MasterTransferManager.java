package protocol;

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

import protocol.constant.MasterSlaveCommunicationStatus;
import protocol.constant.MasterTransferThreadStatus;
import protocol.constant.StatusMapper;

/**
 * Responsible for the full cycle file transfer(from master side). The cycle
 * consists of: (1) health check message (2) metadata message (3) data message
 * (repeats one or more times)
 */
public class MasterTransferManager {

	private Logger logger = LogManager.getRootLogger();

	private BatchFilesTransferOperation bfto;

	private FullFileTransferOperation ffto;

	private SlaveCommunicationPool slaveCommunicationPool;

	private StatusMapper statusMapper;

	// Server socket of the master
	private ServerSocket master;

	// Pool of master-client communication threads
	private ExecutorService slaveThreadPool;

	private MasterTransferThread masterTransferThread;

	public void init(BatchFilesTransferOperation bfto, 
					 FullFileTransferOperation ffto, 
					 SlaveCommunicationPool scp,
					 StatusMapper sm) 
	{
		logger.info("[" + this.getClass().getSimpleName() + "] initialization start");

		this.bfto = bfto;
		this.ffto = ffto;
		this.slaveCommunicationPool = scp;
		this.statusMapper = sm;

		masterTransferThread = new MasterTransferThread();

		try {
			master = new ServerSocket(22222);

			slaveThreadPool = Executors.newSingleThreadExecutor();

//			Thread mtt = new Thread(masterTransferThread);
//			mtt.setName("MasterTransferThread");
//			mtt.start();
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e.getMessage());
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
	 */
	// TODO: Check case when MasterTransferManager status is busy, but new
	// connection is accepted
	private void acceptSlave() {
		Socket slave;
		try {
			logger.info("[" + this.getClass().getSimpleName() + "] waiting for slave to connect");
			slave = master.accept();
			logger.info("[" + this.getClass().getSimpleName() + "] slave connected");

			// Pass os and is to an allocated thread, initial status
			MasterSlaveCommunicationThread communication = new MasterSlaveCommunicationThread(slave);
			slaveThreadPool.execute(communication);

			slaveCommunicationPool.add(communication);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		
		if (status != MasterTransferThreadStatus.EMPTY) {
			List<MasterSlaveCommunicationThread> list = slaveCommunicationPool.get();
			MasterSlaveCommunicationStatus baseValue = list.get(0).getActualStatus();
			status = statusMapper.map(baseValue);

			if (list.size() > 1) {
				for (MasterSlaveCommunicationThread communication : list) {
					if (communication.getActualStatus() != baseValue) {
						status = MasterTransferThreadStatus.TRANSIENT;
					}
				}
			}
		}
		
		return status;
	}

	private void transfer(OutputStream os, InputStream is) {
		ffto.executeAsMaster(os, is);
	}

	public MasterTransferThread getMasterTransferThread() {
		return masterTransferThread;
	}

	public class MasterTransferThread implements Runnable {

		@Override
		public void run() {
			for (;;) {
				acceptSlave();
				// sleep
			}
		}

		public void pause() {
			if (status() != MasterTransferThreadStatus.EMPTY) {
				// Connections that come after this invocation are already in BUSY
				pauseSlaves();
				while (status() != MasterTransferThreadStatus.BUSY) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		/**
		 * resume(), pause() and status() are not invoked simultaneous, only
		 * consequently
		 */
		public void resume() {
			if (status() != MasterTransferThreadStatus.EMPTY) {
				while (status() != MasterTransferThreadStatus.READY) {
					// Multiple invocations to avoid the following situation:
					// 1. Exists one communication in BUSY
					// 2. status() return BUSY
					// 3. new communication BUSY is ADDED
					// 4. status() return BUSY forever because resumeSlave() doesn't consider new communication
					resumeSlaves();
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
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
				e.printStackTrace();
			}

			logger.info("[" + this.getClass().getSimpleName() + "] initialization end");
		}

		@Override
		public void run() {
			logger.info("[" + this.getClass().getSimpleName() + "] transfer start");

			if (actualStatus == MasterSlaveCommunicationStatus.READY) {
				// TODO: Add additional operation get status
				// Check the case
				// requestedStatus = BUSY, actual = READY, thread enters
				// transfer but no transfer is required from
				// slave. For this case get status message should be transfered
				// periodically.
				transfer(os, is);
			} else if (actualStatus == MasterSlaveCommunicationStatus.BUSY) {

			}

			// change status
			if (requestedStatus == MasterSlaveCommunicationStatus.BUSY) {
				actualStatus = MasterSlaveCommunicationStatus.BUSY;
			} else if (requestedStatus == MasterSlaveCommunicationStatus.READY) {
				actualStatus = MasterSlaveCommunicationStatus.READY;
			}

			try {
				os.close();
				is.close();
				slave.close();
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] can't close io-streams / socket",
						e.getMessage());
			}

			logger.info("[" + this.getClass().getSimpleName() + "] transfer end");
		}

		public MasterSlaveCommunicationStatus getActualStatus() {
			return actualStatus;
		}

		public void setRequestedStatus(MasterSlaveCommunicationStatus requestedStatus) {
			this.requestedStatus = requestedStatus;
		}

	}

}
