package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.RepositoryRecord;
import protocol.context.FileContext;
import protocol.constant.MasterStatus;
import protocol.context.EagerFilesContext;
import protocol.context.LazyFilesContext;
import transformer.FilesContextTransformer;

public class SlaveTransferManager {
	
	private BatchFilesTransferOperation bfto;
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	private FullFileTransferOperation ffto;
	
	private StatusTransferOperation sto;
	
	public void init(BatchFilesTransferOperation bfto, 
					 BaseRepositoryOperations bro,  
					 FilesContextTransformer fct, 
					 FullFileTransferOperation ffto,
					 StatusTransferOperation sto) 
	{
		this.bfto = bfto;
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
		try {
			master = new Socket("172.16.42.210", 22222);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new Thread(new SlaveMasterCommunicationThread(master)).start();
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
		
		// TODO: run this on schedule
		ffto.executeAsSlave(os, is, null);
	}

	public Thread getSlaveTransferThread() {
		return new Thread(new SlaveTransferThread());
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
