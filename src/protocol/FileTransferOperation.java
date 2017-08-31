package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import file.FileReceiver;
import file.FileSender;
import protocol.constant.OperationType;
import protocol.context.FileTransferOperationContext;

public class FileTransferOperation {

	//Rename to BaseTransferOperations
	private FileSender bfo1;
	
	private FileReceiver bfo2;
	
	//DELETE AT THE END
	private Path repositoryRoot = Paths.get("D:\\temp");
	private Path p;
	private String fullName;
	private long size;
	private String relativeName;
	// in milliseconds
	private long creationDateTime;
	private Path slaveRepositoryRoot = Paths.get("C:\\temp");
	
	public FileTransferOperation(FileSender bfo1, FileReceiver bfo2) {
		super();
		this.bfo1 = bfo1;
		this.bfo2 = bfo2;
		
		//DELETE AT THE END
		String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
		fullName = "D:\\temp\\" + cyrilicName + ".jpg";
		p = Paths.get(fullName);
		try {
			size = Files.readAttributes(p, BasicFileAttributes.class).size();
			relativeName = repositoryRoot.relativize(p).toString();
			creationDateTime = Files.readAttributes(p, BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void executeAsMaster(OutputStream os, InputStream is, FileTransferOperationContext ftoc){
		//Receive request for a file
		OperationType ot = bfo2.receiveActionType(is);
		if(ot != OperationType.REQUEST_FILE_START) {
			//error detected
		}
		Path p = bfo2.receiveRelativeName(is);
		ot = bfo2.receiveActionType(is);
		if(ot != OperationType.REQUEST_FILE_END) {
			//error detected
		}
		
		//Send the requested file back
		bfo1.sendActionType(os, OperationType.RESPONSE_FILE_START);
		bfo1.sendSize(os, size);
		bfo1.sendRelativeName(os, relativeName);
		bfo1.sendCreationDate(os, creationDateTime);
		bfo1.send(os, Paths.get(repositoryRoot.toString(), p.toString()));
		bfo1.sendActionType(os, OperationType.RESPONSE_FILE_END);
	}

	public void executeAsSlave(OutputStream os, InputStream is, FileTransferOperationContext ftoc){
		//Change signature to (OutputStream, operation_type)
		bfo1.sendActionType(os, OperationType.REQUEST_FILE_START);	
		bfo1.sendRelativeName(os, relativeName);
		bfo1.sendActionType(os, OperationType.REQUEST_FILE_END);
		
		OperationType ot = bfo2.receiveActionType(is);
		if(ot != OperationType.RESPONSE_FILE_START) {
			//error detected
		}
		long size = bfo2.receiveSize(is);
		Path p = bfo2.receiveRelativeName(is);
		long creationDateTime = bfo2.receiveCreationDate(is);
		bfo2.receive(is, size, slaveRepositoryRoot, p, creationDateTime);
		ot = bfo2.receiveActionType(is);
		if(ot != OperationType.RESPONSE_FILE_END) {
			//error detected
		}
	}
	
}
