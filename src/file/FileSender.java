package file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import protocol.file.FrameProcessor;

public class FileSender {

	private Path p;
	private long size;
	private String relativeName;
	// in milliseconds
	private long creationDateTime;
	
	// Move to properties
	private Path repositoryRoot = Paths.get("D:\\temp");
	
	private FrameProcessor fp = new FrameProcessor();

	public FileSender(String fullName) throws IOException {
		p = Paths.get(fullName);
		size = Files.readAttributes(p, BasicFileAttributes.class).size();
		relativeName = repositoryRoot.relativize(p).toString();
		creationDateTime = Files.readAttributes(p, BasicFileAttributes.class).creationTime().toMillis();
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
	
	public void sendRelativeName(OutputStream os) {
		int length = 0;
		byte[] b;
		try {
			b = relativeName.getBytes("UTF-8");
			length = b.length;
			os.write(length);
			os.write(b);
			
			//Only 7 bits of the byte are used for size of the name representation
			assert(length < 128);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendCreationDate(OutputStream os) {
		try {
			os.write(fp.packSize(creationDateTime));
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
