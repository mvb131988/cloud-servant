package file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import protocol.file.FrameProcessor;

public class FileReceiver {

	private FrameProcessor fp = new FrameProcessor();
	
	// TODO: Move out
	public int receiveActionType(InputStream is) {
		int actionType = -1;
		try {
			// file transfer operation
			actionType = is.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return actionType;
	}

	public long receiveSize(InputStream is) {
		long assembledSize = -1;
		try {
			byte[] size = new byte[8];
			is.read(size);
			assembledSize = fp.extractSize(size);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return assembledSize;
	}

	//TODO: while for receive
	public void receive(InputStream is, long size) {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		
		long remainigSize = size;
		
		Path p = Paths.get("C:\\temp\\raft.pdf");
		try (OutputStream os = Files.newOutputStream(p);) {
			while(remainigSize != 0) {
				readBufferSize = remainigSize >= 1024 ? is.read(buffer, 0, 1024) : is.read(buffer, 0, (int) remainigSize);
				remainigSize -= readBufferSize;
				os.write(buffer, 0, readBufferSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
