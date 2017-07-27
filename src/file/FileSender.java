package file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import protocol.file.FrameProcessor;

public class FileSender {

	private Path p;
	private long size;
	
	private FrameProcessor fp = new FrameProcessor();

	public FileSender(String fullName) throws IOException {
		p = Paths.get(fullName);
		size = Files.readAttributes(p, BasicFileAttributes.class).size();
	}
	
	//TODO: Move out
	public void sendActionType(OutputStream os){
		try {
			// file transfer operation
			os.write(0x01);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendSize(OutputStream os) {
		try {
			os.write(fp.packSize(size));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//TODO: while for send
	public void send(OutputStream os) {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		try (InputStream is = Files.newInputStream(p);) {
			while((readBufferSize = is.read(buffer))!=-1){
				os.write(buffer, 0, readBufferSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
