package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import main.AppProperties;
import repository.BaseRepositoryOperations;
import transfer.constant.OperationType;
import transfer.context.FileContext;

/**
 *	Implements transfer protocol for a single file. Both master and slave sides.
 */
public class FileTransferOperation {
	
	private Path repositoryRoot;
	
	private Logger logger = LogManager.getRootLogger();

	private BaseTransferOperations bto;
	
	private BaseRepositoryOperations bro;
	
	public FileTransferOperation(BaseTransferOperations bto, 
								 BaseRepositoryOperations bro, 
								 AppProperties appProperties) {
		super();
		this.bto = bto;
		this.bro = bro;
		this.repositoryRoot = appProperties.getRepositoryRoot();
	}

	public void inbound(OutputStream os, InputStream is) 
			throws IOException, WrongOperationException
	{
		//Receive request for a file
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_START) {
			throw new WrongOperationException(
					"Expected: " + OperationType.REQUEST_FILE_START + " Actual: " + ot);
		}
		logger.trace("[" + this.getClass().getSimpleName() + "] inbound member requested file "
				   + "start operation");
		
		Path relativePath = bto.receiveRelativePath(is);
		
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.REQUEST_FILE_END) {
			throw new WrongOperationException(
					"Expected: " + OperationType.REQUEST_FILE_END + " Actual: " + ot);
		}
		
		//Send the requested file back
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_START);
		long size = bro.getSize(relativePath);
		bto.sendSize(os, size);
		bto.sendRelativePath(os, relativePath);
		bto.sendCreationDateTime(os, bro.getCreationDateTime(relativePath));
		bto.sendFile(os, repositoryRoot.resolve(relativePath).normalize());
		bto.sendOperationType(os, OperationType.RESPONSE_FILE_END);

		logger.trace("[" + this.getClass().getSimpleName() + "] file[" + relativePath + "] "
				   + "size[" + size + "bytes] was sent");
	}

	public void outbound(OutputStream os, InputStream is, FileContext fc) 
			throws IOException, WrongOperationException
	{
		bto.sendOperationType(os, OperationType.REQUEST_FILE_START);	
		bto.sendRelativePath(os, fc.getRelativePath());
		bto.sendOperationType(os, OperationType.REQUEST_FILE_END);
		logger.trace("[" + this.getClass().getSimpleName() + "] sent request for file [" 
				   + fc.getRelativePath() + "]" );
		
		OperationType ot = bto.receiveOperationType(is);
		if(ot != OperationType.RESPONSE_FILE_START) {
			throw new WrongOperationException(
					"Expected: " + OperationType.RESPONSE_FILE_START + " Actual: " + ot);
		}
		
		long size = bto.receiveSize(is);
		Path relativePath = bto.receiveRelativePath(is);
		long creationDateTime = bto.receiveCreationDateTime(is);
		bto.receiveFile(is, size, repositoryRoot, relativePath, creationDateTime);
		
		ot = bto.receiveOperationType(is);
		if(ot != OperationType.RESPONSE_FILE_END) {
			throw new WrongOperationException(
					"Expected: " + OperationType.RESPONSE_FILE_END + " Actual: " + ot);
		}
		
		logger.trace("[" + this.getClass().getSimpleName() + "] file[" + relativePath 
				   + "] size[" + size + "bytes] received");
	}
	
}
