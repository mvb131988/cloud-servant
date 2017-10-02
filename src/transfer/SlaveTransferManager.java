package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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
	
	private void connect() {
		Socket master = null;
		String ip = "172.16.42.210";
		int port = 22222;
		
		try {
			logger.info("[" + this.getClass().getSimpleName() + "] opening socket to " + ip + ":" + port);
			master = new Socket(ip, port);
			logger.info("[" + this.getClass().getSimpleName() + "]  socket to " + ip + ":" + port + " opened");
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] error opening socket to " + ip + ":" + port);
		}
		
		Thread thread = new Thread(new SlaveMasterCommunicationThread(master));
		thread.setName("SlaveTransferThread");
		thread.start();
	}
	
	private void transfer(OutputStream os, InputStream is) {
		MasterStatus status = null;
		while((status = sto.executeAsSlave(os, is)) == MasterStatus.BUSY) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
			connect();
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
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			transfer(os, is);

			try {
				os.close();
				is.close();
				master.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
