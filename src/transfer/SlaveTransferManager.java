package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.BaseRepositoryOperations;
import transfer.constant.MasterStatus;
import transformer.FilesContextTransformer;

public class SlaveTransferManager {
	
	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	private FullFileTransferOperation ffto;
	
	private StatusTransferOperation sto;
	
	public void init(BaseRepositoryOperations bro,  
					 FilesContextTransformer fct, 
					 FullFileTransferOperation ffto,
					 StatusTransferOperation sto) 
	{
		this.bro = bro;
		this.fct = fct;
		this.ffto = ffto;
		this.sto = sto;
	}

	public void destroy() {
		//wait/stop SlaveMasterCommunicationThread
		//stop SlaveTransferThread
	}
	
	private void connect() throws UnknownHostException, IOException {
		Socket master = null;
		String ip = "172.16.42.210";
		int port = 22222;
		
		logger.info("[" + this.getClass().getSimpleName() + "] opening socket to " + ip + ":" + port);
		master = new Socket(ip, port);
		logger.info("[" + this.getClass().getSimpleName() + "]  socket to " + ip + ":" + port + " opened");
		
		Thread thread = new Thread(new SlaveMasterCommunicationThread(master));
		thread.setName("SlaveTransferThread");
		thread.start();
	}
	
	private void transfer(OutputStream os, InputStream is) throws InterruptedException, IOException {
		MasterStatus status = null;
		while((status = sto.executeAsSlave(os, is)) == MasterStatus.BUSY) {
			Thread.sleep(1000);
		}
		
		// TODO(NORMAL): run this on schedule

		ffto.executeAsSlave(os, is);
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
			try {
				connect();
			} catch (Exception e) {
				//TODO: Log exception
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
				//TODO: Log exception
			}
		}

		@Override
		public void run() {
			try {
				transfer(os, is);
				
				os.close();
				is.close();
				master.close();
			} catch (Exception e) {
				//TODO: Log exception
			}
		}

	}

}
