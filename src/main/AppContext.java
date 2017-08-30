package main;

import java.io.IOException;

import file.FileReceiver;
import file.FileSender;
import file.repository.metadata.RepositoryManager;
import file.repository.metadata.RepositoryVisitor;
import protocol.MasterTransferManager;
import protocol.SlaveTransferManager;
import protocol.file.FrameProcessor;

public class AppContext {

	private FrameProcessor fp = new FrameProcessor();
	
	private boolean isMaster = true;

	public void start() {
		if (isMaster) {
			startAsServer();
		} else {
			startAsClient();
		}
	}

	private void startAsServer() {
		//scan repository and create data.repository
		Thread masterRepositoryThread = getRepositoryManager().getMasterRepositoryThread();
		masterRepositoryThread.start();
		try {
			masterRepositoryThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//only after scan repository thread is finished start master file transferring component
		getMasterTransferManager().init(getFileSender());
	}

	private void startAsClient() {
		getSlaveTransferManager().init(getFileReceiver());
	}

	
	private RepositoryManager repositoryManager = new RepositoryManager();
	public RepositoryManager getRepositoryManager() {
		return new RepositoryManager();
	}

	// singleton scope
	private RepositoryVisitor repositoryVisitor = new RepositoryVisitor();

	public RepositoryVisitor getRepositoryVisitor() {
		return repositoryVisitor;
	}

	private MasterTransferManager masterTransferManager = new MasterTransferManager();

	public MasterTransferManager getMasterTransferManager() {
		return masterTransferManager;
	}

	private SlaveTransferManager slaveTransferManager = new SlaveTransferManager();

	public SlaveTransferManager getSlaveTransferManager() {
		return slaveTransferManager;
	}

	private FileSender fileSender;

	private FileSender getFileSender() {
		if (fileSender == null) {
			String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
			try {
				fileSender = new FileSender("D:\\temp\\" + cyrilicName + ".jpg");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fileSender;
	}
	
	private FileReceiver fileReceiver = new FileReceiver();
	
	private FileReceiver getFileReceiver() {
		return fileReceiver;
	}

}
