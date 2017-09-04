package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

import protocol.context.FileContext;
import protocol.context.FilesContext;

public class SlaveTransferManager {
	
	private BatchFilesTransferOperation bfto;
	
	public void init(BatchFilesTransferOperation bfto) {
		this.bfto = bfto;
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
	
	// Transfer protocol logic method. Contains 3 major steps:
	// (1) health check message
	// (2) metadata message
	// (3) data message (repeats one or more times) 
	private void transfer(OutputStream os, InputStream is) {
//		String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
		
		Path repositoryRoot = Paths.get("C:\\temp");
//		Path relativePath = Paths.get(cyrilicName + ".jpg");
		Path relativePath = Paths.get("data.repo");
		
		FilesContext fsc = new FilesContext();
		
		FileContext fc = (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(relativePath)
				.build(); 
		fsc.add(fc);
		
//		relativePath = Paths.get("31e38af422fc7dac65b484aa81921afa.jpg");
//		fc = (new FileContext.Builder())
//				.setRepositoryRoot(repositoryRoot)
//				.setRelativePath(relativePath)
//				.build(); 
//		fsc.add(fc);
		
		bfto.executeAsSlave(os, is, fsc);
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
