package protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import protocol.constant.OperationType;
import protocol.context.FileContext;
import protocol.context.FileTransferOperationContext;
import protocol.context.FilesContext;

/**
 *	Implements transfer protocol for a single file. Both master and slave sides. 
 */
public class FileTransferOperation {

	private BaseTransferOperations bto;
	
	public FileTransferOperation(BaseTransferOperations bto) {
		super();
		this.bto = bto;
	}

	public void executeAsMaster(OutputStream os, InputStream is, FileContext fc){
		//Receive request for a file
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_START) {
			//error detected
		}
		Path relativePath = bto.receiveRelativePath(is);
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_END) {
			//error detected
		}
		
		//File Lookup by relative name
		
		//Send the requested file back
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_START);
		bto.sendSize(os, fc.getSize());
		bto.sendRelativePath(os, fc.getRelativePath());
		bto.sendCreationDateTime(os, fc.getCreationDateTime());
		bto.sendFile(os, fc.getRepositoryRoot().resolve(relativePath));
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_END);
	}

	public void executeAsSlave(OutputStream os, InputStream is, FileContext fc){
		//Change signature to (OutputStream, operation_type)
		bto.sendOperationType(os, OperationType.REQUEST_FILE_START);	
		bto.sendRelativePath(os, fc.getRelativePath());
		bto.sendOperationType(os, OperationType.REQUEST_FILE_END);
		
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.RESPONSE_FILE_START) {
			//error detected
		}
		long size = bto.receiveSize(is);
		Path relativePath = bto.receiveRelativePath(is);
		long creationDateTime = bto.receiveCreationDateTime(is);
		bto.receiveFile(is, size, fc.getRepositoryRoot(), relativePath, creationDateTime);
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.RESPONSE_FILE_END) {
			//error detected
		}
	}
	
}
