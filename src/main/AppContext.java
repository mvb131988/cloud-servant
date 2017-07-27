package main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import file.FileReceiver;
import file.FileSender;
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
		int v;
		try (ServerSocket serverSocket = new ServerSocket(22222)) {
			Socket clientSocket = serverSocket.accept();
			OutputStream os = clientSocket.getOutputStream();
			
			FileSender fs = new FileSender("D:\\temp\\raft.pdf");
			fs.sendActionType(os);
			fs.sendSize(os);
			fs.send(os);
			
			os.close();
			clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startAsClient() {
		Socket echoSocket;
		
		try {
			echoSocket = new Socket("192.168.47.132", 22222);
			InputStream is = echoSocket.getInputStream();
			
			FileReceiver fr = new FileReceiver();
			fr.receiveActionType(is);
			long size = fr.receiveSize(is);
			fr.receive(is, size);
			
			is.close();
			echoSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
