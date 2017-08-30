package protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import file.FileReceiver;
import file.FileSender;

public class FileTransferOperation {

	//Rename to BaseTransferOperations
	private FileSender bfo1;
	
	private FileReceiver bfo2;
	
	public FileTransferOperation(FileSender bfo1, FileReceiver bfo2) {
		super();
		this.bfo1 = bfo1;
		this.bfo2 = bfo2;
	}

	public void executeAsMaster(OutputStream os, InputStream is, FileTransferOperationContext ftoc){
		bfo2.receiveActionType(is);
		Path p = bfo2.receiveRelativeName(is);
		bfo2.receiveActionType(is);
		
		bfo1.sendActionType(os);
		bfo1.sendSize(os);
		bfo1.sendRelativeName(os);
		bfo1.sendCreationDate(os);
		bfo1.send(os);
		bfo1.sendActionType(os);
	}

	public void executeAsSlave(OutputStream os, InputStream is, FileTransferOperationContext ftoc){
		//Change signature to (OutputStream, operation_type)
		bfo1.sendActionType(os);	
		bfo1.sendRelativeName(os);
		bfo1.sendActionType(os);
		
		bfo2.receiveActionType(is);
		long size = bfo2.receiveSize(is);
		Path p = bfo2.receiveRelativeName(is);
		long creationDateTime = bfo2.receiveCreationDate(is);
		bfo2.receive(is, size, p, creationDateTime);
		bfo2.receiveActionType(is);
	}
	
}
