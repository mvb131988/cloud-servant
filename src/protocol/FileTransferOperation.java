package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import protocol.constant.OperationType;
import protocol.context.FileTransferOperationContext;

/**
 *	Implements transfer protocol for a single file. Both master and slave sides. 
 */
public class FileTransferOperation {

	private BaseTransferOperations bto;
	
	//----DELETE AT THE END
	private Path repositoryRoot = Paths.get("D:\\temp");
	private Path p;
	private String fullName;
	private long size;
	private Path relativeName;
	// in milliseconds
	private long creationDateTime;
	private Path slaveRepositoryRoot = Paths.get("C:\\temp");
	//----DELETE
	
	public FileTransferOperation(BaseTransferOperations bto) {
		super();
		this.bto = bto;
		
		//----DELETE AT THE END
		String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
		fullName = "D:\\temp\\" + cyrilicName + ".jpg";
		p = Paths.get(fullName);
		try {
			size = Files.readAttributes(p, BasicFileAttributes.class).size();
			relativeName = repositoryRoot.relativize(p);
			creationDateTime = Files.readAttributes(p, BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//----DELETE
	}

	public void executeAsMaster(OutputStream os, InputStream is, FileTransferOperationContext ftoc){
		//Receive request for a file
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_START) {
			//error detected
		}
		Path p = bto.receiveRelativePath(is);
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_END) {
			//error detected
		}
		
		//Send the requested file back
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_START);
		bto.sendSize(os, size);
		bto.sendRelativePath(os, relativeName);
		bto.sendCreationDateTime(os, creationDateTime);
		bto.sendFile(os, Paths.get(repositoryRoot.toString(), p.toString()));
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_END);
	}

	public void executeAsSlave(OutputStream os, InputStream is, FileTransferOperationContext ftoc){
		//Change signature to (OutputStream, operation_type)
		bto.sendOperationType(os, OperationType.REQUEST_FILE_START);	
		bto.sendRelativePath(os, relativeName);
		bto.sendOperationType(os, OperationType.REQUEST_FILE_END);
		
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.RESPONSE_FILE_START) {
			//error detected
		}
		long size = bto.receiveSize(is);
		Path p = bto.receiveRelativePath(is);
		long creationDateTime = bto.receiveCreationDateTime(is);
		bto.receiveFile(is, size, slaveRepositoryRoot, p, creationDateTime);
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.RESPONSE_FILE_END) {
			//error detected
		}
	}
	
}
