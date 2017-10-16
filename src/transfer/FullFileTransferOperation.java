package transfer;

import static transfer.constant.OperationType.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;
import transfer.context.FileContext;

/**
 * Main operation of the protocol. Organize main protocol cycle, which handles all supported operations.  
 * This operation is non-interruptible. MasterTransferManager must wait until it completely finishes.
 */
public class FullFileTransferOperation {
	
	private Logger logger = LogManager.getRootLogger();

	private BaseTransferOperations bto;
	
	private FileTransferOperation fto;
	
	private StatusTransferOperation sto;
	
	private BatchFilesTransferOperation bfto;
	
	public FullFileTransferOperation(BaseTransferOperations bto, 
									 FileTransferOperation fto, 
									 StatusTransferOperation sto,
									 BatchFilesTransferOperation bfto) {
		super();
		this.bto = bto;
		this.fto = fto;
		this.sto = sto;
		this.bfto = bfto;
	}

	public void executeAsMaster(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(is);

		OperationType ot = bto.checkOperationType(pushbackInputStream);
		
		//If first received operation is status check, return the status and leave immediately.
		//Otherwise thread will return status check(and in this time it couldn't be paused), 
		//until full file transfer operation isn't received. If full file transfer operation is scheduled
		// to be performed once in a day it will block RepositoryScanner thread.
		//TODO(MAJOR): instead of REQUEST_MASTER_STATUS_START create new HEALTHCHECK REQUEST
		if(ot == REQUEST_MASTER_STATUS_START) {
			sto.executeAsMaster(os, pushbackInputStream, MasterStatus.READY);
			logger.info("[" + this.getClass().getSimpleName() + "] slave requested status");
			return;
		}
		
		while (REQUEST_TRANSFER_END != (ot = bto.checkOperationType(pushbackInputStream))) {
			if (ot == null) {
				// if connection is aborted
				// error detected
			}

			switch (ot) {
			case REQUEST_MASTER_STATUS_START: 
				sto.executeAsMaster(os, pushbackInputStream, MasterStatus.READY);
				break;
			case REQUEST_TRANSFER_START:
				bto.receiveOperationType(pushbackInputStream);
				logger.info("[" + this.getClass().getSimpleName() + "] slave requested transfer start operation");

				bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_START);
				logger.info("[" + this.getClass().getSimpleName() + "] sent transfer start operation accept");
				break;
			case REQUEST_BATCH_START:
				logger.info("[" + this.getClass().getSimpleName() + "] batch transfer started");
				bfto.executeAsMaster(os, pushbackInputStream);
				logger.info("[" + this.getClass().getSimpleName() + "] batch transfer ended");
				break;
			case REQUEST_FILE_START:
				logger.info("[" + this.getClass().getSimpleName() + "] file transfer started");
				fto.executeAsMaster(os, pushbackInputStream);
				logger.info("[" + this.getClass().getSimpleName() + "] file transfer ended");
				break;
			default:
				throw new WrongOperationException();
			}
		}
		// read REQUEST_TRANSFER_END byte
		bto.receiveOperationType(pushbackInputStream);
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested transfer end operation");

		bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer end operation accept");
	}
	
	public void executeAsSlave(OutputStream os, InputStream is) throws InterruptedException, IOException, MasterNotReadyDuringBatchTransfer, WrongOperationException {
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer start operation request");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_START) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_TRANSFER_START + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded transfer start");
		
		// Get data.repo
		Path relativePath = Paths.get("data.repo");
		FileContext fc = (new FileContext.Builder())
				.setRelativePath(relativePath)
				.build(); 
		fto.executeAsSlave(os, is, fc);
		
		// batch transfer
		bfto.executeAsSlave(os, is);
		
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer end request");
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_TRANSFER_END + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded transfer end");
	}
	
}
