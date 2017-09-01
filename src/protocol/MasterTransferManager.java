package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import protocol.context.FileContext;
import protocol.context.FilesContext;

/**
 *  Responsible for the full cycle file transfer(from master side). 
 *  The cycle consists of:
 *  (1) health check message
 *	(2) metadata message
 *	(3) data message (repeats one or more times) 
 */
public class MasterTransferManager {

	private BatchFilesTransferOperation bfto;
	
	// Server socket of the master
	private ServerSocket master;

	// Pool of master-client communication threads
	private ExecutorService slavePool;
	
	public void init(BatchFilesTransferOperation bfto) {
		this.bfto = bfto;
		
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
		String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
		
		Path repositoryRoot = Paths.get("D:\\temp");
		Path relativePath = Paths.get(cyrilicName + ".jpg");
		long size = 0;
		long creationDateTime = 0;
		try {
			size = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).size();
			creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileContext fc = (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(relativePath)
				.setSize(size)
				.setCreationDateTime(creationDateTime)
				.build(); 
		
		FilesContext fsc = new FilesContext();
		fsc.add(fc);
		
		bfto.executeAsMaster(os, is, fsc);
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
