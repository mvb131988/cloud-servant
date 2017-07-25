package main;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import protocol.file.FrameProcessor;

public class Main {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
//	    try {
//			ServerSocket serverSocket = new ServerSocket(22222);
//			Socket clientSocket = serverSocket.accept();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		FrameProcessor fp = new FrameProcessor(); 
		byte[] packedSize = fp.packSize(15_260_287l);
		long extractedSize = fp.extractSize(packedSize);
		
		Socket echoSocket;
		
		//file transfer operation
		byte operationType = 0x01;
		
		try {
			echoSocket = new Socket("192.168.47.132", 22222);
			OutputStream os = echoSocket.getOutputStream();
			byte[] out = new byte[]{0x1a,0x1b,0x1c,0x1d,0x1e};
			os.write(operationType);
			os.write(out);
			os.close();
			echoSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
