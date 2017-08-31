package file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import protocol.constant.OperationType;
import protocol.file.FrameProcessor;

//TODO:
//Rename to BaseTransferOperations
public class FileSender {
	
	private FrameProcessor fp = new FrameProcessor();

	//TODO: Move out
	public void sendActionType(OutputStream os, OperationType ot){
		try {
			// file transfer operation
			os.write(ot.getType());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendSize(OutputStream os, long size) {
		try {
			os.write(fp.packSize(size));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//TODO: Extend relative name size to int
	public void sendRelativeName(OutputStream os, String relativePath) {
		int length = 0;
		byte[] b;
		try {
			b = relativePath.getBytes("UTF-8");
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
	
	public void sendCreationDate(OutputStream os, long creationDateTime) {
		try {
			os.write(fp.packSize(creationDateTime));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//TODO: while for send
	public void send(OutputStream os, Path absolutePath) {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		try (InputStream is = Files.newInputStream(absolutePath);) {
			while((readBufferSize = is.read(buffer))!=-1){
				os.write(buffer, 0, readBufferSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
