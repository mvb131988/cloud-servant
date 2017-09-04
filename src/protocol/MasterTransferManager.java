package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *  Responsible for the full cycle file transfer(from master side). 
 *  The cycle consists of:
 *  (1) health check message
 *	(2) metadata message
 *	(3) data message (repeats one or more times) 
 */
public class MasterTransferManager {

	private BatchFilesTransferOperation bfto;
	
	private FullFileTransferOperation ffto;
	
	// Server socket of the master
	private ServerSocket master;

	// Pool of master-client communication threads
	private ExecutorService slavePool;
	
	public void init(BatchFilesTransferOperation bfto, FullFileTransferOperation ffto) {
		this.bfto = bfto;
		this.ffto = ffto;
		
		try {
			master = new ServerSocket(22222);
			slavePool = Executors.newSingleThreadExecutor();

			new Thread(new MasterTransferThread()).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			slave = master.accept();
			
			// Pass os and is to an allocated thread
			MasterSlaveCommunicationThread communication = new MasterSlaveCommunicationThread(slave);
			slavePool.execute(communication);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Transfer protocol logic method. Contains 3 major steps:
	// (1) health check message
	// (2) metadata message
	// (3) data message (repeats one or more times) 
	private void transfer(OutputStream os, InputStream is) {
//		bfto.executeAsMaster(os, is);
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
			
			this.slave = slave;
			
			try {
				this.os = slave.getOutputStream();
				this.is = slave.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			transfer(os, is);
			
			try {
				os.close();
				is.close();
				slave.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
