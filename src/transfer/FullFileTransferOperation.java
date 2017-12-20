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

import exception.BatchFileTransferException;
import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import repository.BaseRepositoryOperations;
import repository.status.RepositoryFileStatus;
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
	
	private BaseRepositoryOperations bro;
	
	private FileTransferOperation fto;
	
	private StatusTransferOperation sto;
	
	private BatchFilesTransferOperation bfto;
	
	private HealthCheckOperation hco;
	
	public FullFileTransferOperation(BaseTransferOperations bto, 
									 BaseRepositoryOperations bro,
									 FileTransferOperation fto, 
									 StatusTransferOperation sto,
									 HealthCheckOperation hco,
									 BatchFilesTransferOperation bfto) 
	{
		super();
		this.bto = bto;
		this.bro = bro;
		this.fto = fto;
		this.sto = sto;
		this.bfto = bfto;
		this.hco = hco;
	}

	public void executeAsMaster(OutputStream os, PushbackInputStream pushbackInputStream) throws IOException, WrongOperationException {
		OperationType ot = bto.checkOperationType(pushbackInputStream);
		
		//If firstly health check request is coming(master slave communication thread in READY), then status is returned, but
		//no transfer process is started. Immediately after that(if master slave communication thread remains READY) status
		//check request is coming. This request grabs master slave communication thread(in this moment it status can't be changed
		//until full file transfer is completed) and initiates full file transfer operation.
		if(ot == REQUEST_HEALTHCHECK_START) {
			hco.executeAsMaster(os, pushbackInputStream, MasterStatus.READY);
			logger.info("[" + this.getClass().getSimpleName() + "] slave requested healthcheck");
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
	
	public void executeAsSlave(OutputStream os, InputStream is) throws InterruptedException, IOException, MasterNotReadyDuringBatchTransfer, WrongOperationException, BatchFileTransferException {
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
		bro.updateDataRepoStatus(RepositoryFileStatus.RECEIVE_END);
		
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
