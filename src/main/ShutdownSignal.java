package main;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Signals application to start a shutdown 
 */
public class ShutdownSignal {

	private int adminPort;
	
	public ShutdownSignal(AppProperties appProperties) {
		adminPort = appProperties.getAdminPort();
	}
	
	public void send() {
		try {
			Socket socket = new Socket("127.0.0.1", adminPort);
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
