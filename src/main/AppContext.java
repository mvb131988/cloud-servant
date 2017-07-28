package main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

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
			
			String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
			FileSender fs = new FileSender("D:\\temp\\" + cyrilicName + ".jpg");
			fs.sendActionType(os);
			fs.sendSize(os);
			fs.sendRelativeName(os);
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
			Path p = fr.receiveRelativeName(is);
			long creationDateTime = fr.receiveCreationDate(is);
			fr.receive(is, size, p, creationDateTime);
			
			is.close();
			echoSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
