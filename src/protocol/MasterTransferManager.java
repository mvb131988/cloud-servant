package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *  Responsible for the full cycle file transfer(from master side). 
 *  The cycle consists of:
 *  (1) health check message
 *	(2) metadata message
 *	(3) data message (repeats one or more times) 
 */
public class MasterTransferManager {

	private Logger logger = LogManager.getRootLogger();
	
	private BatchFilesTransferOperation bfto;
	
	private FullFileTransferOperation ffto;
	
	// Server socket of the master
	private ServerSocket master;

	// Pool of master-client communication threads
	private ExecutorService slavePool;
	
	public void init(BatchFilesTransferOperation bfto, FullFileTransferOperation ffto) {
		logger.info("[" + this.getClass().getSimpleName() + "] initialization start");

		this.bfto = bfto;
		this.ffto = ffto;

		try {
			master = new ServerSocket(22222);
			
			slavePool = Executors.newSingleThreadExecutor();

			Thread mtt = new Thread(new MasterTransferThread());
			mtt.setName("MasterTransferThread");
			mtt.start();
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e.getMessage());
		}

		logger.info("[" + this.getClass().getSimpleName() + "] initialization end");
	}

	public void destroy() {
		//stop MasterTransferThread
		//close server socket
		//close thread pool 
	}
	
	/**
	 * Establishes connection with the slave and passes client socket to a separate thread of execution
	 */
	private void acceptSlave() {
		Socket slave;
		try {
			logger.info("[" + this.getClass().getSimpleName() + "] waiting for slave to connect");
			slave = master.accept();
			logger.info("[" + this.getClass().getSimpleName() + "] slave connected");
			
			// Pass os and is to an allocated thread
			MasterSlaveCommunicationThread communication = new MasterSlaveCommunicationThread(slave);
			slavePool.execute(communication);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void transfer(OutputStream os, InputStream is) {
		ffto.executeAsMaster(os, is);
	}
	
	private class MasterTransferThread implements Runnable {

		@Override
		public void run() {
			for(;;) {
				acceptSlave();
			}
		}
		
	}
	
	private class MasterSlaveCommunicationThread implements Runnable {
		
		private Socket slave;
		
		private OutputStream os;
		
		private InputStream is;
		
		public MasterSlaveCommunicationThread(Socket slave) {
			super();
			
			logger.info("[" + this.getClass().getSimpleName() + "] initialization start");
			
			this.slave = slave;
			
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
			
			transfer(os, is);
			
			try {
				os.close();
				is.close();
				slave.close();
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] can't close io-streams / socket", e.getMessage());
			}
			
			logger.info("[" + this.getClass().getSimpleName() + "] transfer end");
		}
		
	}

}
