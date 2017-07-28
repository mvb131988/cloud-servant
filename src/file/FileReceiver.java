package file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import protocol.file.FrameProcessor;

public class FileReceiver {

	private FrameProcessor fp = new FrameProcessor();
	
	// Move to properties
	private Path repositoryRoot = Paths.get("C:\\temp");
	
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

	public long receiveCreationDate(InputStream is) {
		long assembledCreationDateTime = -1;
		try {
			byte[] creationDateTime = new byte[8];
			is.read(creationDateTime);
			assembledCreationDateTime = fp.extractSize(creationDateTime);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return assembledCreationDateTime;
	}
	
	public Path receiveRelativeName(InputStream is) {
		Path p = null;
		try {
			// relativeNameSize
			int rns = is.read();
			// relativeName
			byte[] rn = new byte[rns];
			is.read(rn, 0, rns);
			
			p = Paths.get(new String(rn, "UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return p;
	}
	
	//TODO: while for receive
	public void receive(InputStream is, long size, Path relativeFilePath, long creationDateTime) {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		long remainigSize = size;
		
		Path p = repositoryRoot.resolve(relativeFilePath);
		
		try (OutputStream os = Files.newOutputStream(p);) {
			while(remainigSize != 0) {
				readBufferSize = remainigSize >= 1024 ? is.read(buffer, 0, 1024) : is.read(buffer, 0, (int) remainigSize);
				remainigSize -= readBufferSize;
				os.write(buffer, 0, readBufferSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Files.setAttribute(p, "creationTime", FileTime.fromMillis(creationDateTime));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
