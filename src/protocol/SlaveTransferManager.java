package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;

import file.FileReceiver;

public class SlaveTransferManager {

	//HealthCheck, TransferData, TransferMetadata operation must be here
	private FileReceiver fileReceiver;
	
	public void init(FileReceiver fileReceiver) {
		this.fileReceiver = fileReceiver;
		
		new Thread(new SlaveTransferThread()).start();
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
		fileReceiver.receiveActionType(is);
		long size = fileReceiver.receiveSize(is);
		Path p = fileReceiver.receiveRelativeName(is);
		long creationDateTime = fileReceiver.receiveCreationDate(is);
		fileReceiver.receive(is, size, p, creationDateTime);
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
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
