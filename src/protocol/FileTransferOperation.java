package protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.FilePropertyLookupService;
import protocol.constant.OperationType;
import protocol.context.FileContext;

/**
 *	Implements transfer protocol for a single file. Both master and slave sides. 
 */
public class FileTransferOperation {
	
	private Logger logger = LogManager.getRootLogger();

	private BaseTransferOperations bto;
	
	private FilePropertyLookupService fpls;
	
	public FileTransferOperation(BaseTransferOperations bto, FilePropertyLookupService fpls) {
		super();
		this.bto = bto;
		this.fpls = fpls;
	}

	public void executeAsMaster(OutputStream os, InputStream is){
		//Receive request for a file
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_START) {
			//error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested file start operation");
		
		Path relativePath = bto.receiveRelativePath(is);
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_END) {
			//error detected
		}
		
		//Send the requested file back
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_START);
		long size = fpls.getSize(relativePath);
		bto.sendSize(os, size);
		bto.sendRelativePath(os, relativePath);
		bto.sendCreationDateTime(os, fpls.getCreationDateTime(relativePath));
		bto.sendFile(os, fpls.getRepositoryRoot().resolve(relativePath).normalize());
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_END);

		logger.info("[" + this.getClass().getSimpleName() + "] file[" + relativePath + "] size[" + size + "bytes] was sent");
	}

	public void executeAsSlave(OutputStream os, InputStream is, FileContext fc){
		bto.sendOperationType(os, OperationType.REQUEST_FILE_START);	
		bto.sendRelativePath(os, fc.getRelativePath());
		bto.sendOperationType(os, OperationType.REQUEST_FILE_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent request for file [" + fc.getRelativePath() + "]" );
		
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
		
		logger.info("[" + this.getClass().getSimpleName() + "] file[" + relativePath + "] size[" + size + "bytes] received");
	}
	
}
