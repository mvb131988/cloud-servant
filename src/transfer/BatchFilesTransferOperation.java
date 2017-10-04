package transfer;

import static transfer.constant.OperationType.REQUEST_BATCH_END;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.MasterNotReadyDuringBatchTransfer;
import repository.RepositoryRecord;
import repository.SlaveRepositoryManager;
import repository.status.SlaveRepositoryManagerStatus;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;
import transformer.FilesContextTransformer;

/**
 * Implements transfer protocol for a batch of files. Both master and slave
 * sides.
 */
public class BatchFilesTransferOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private FileTransferOperation fto;

	private BaseTransferOperations bto;
	
	private FilesContextTransformer fct;
	
	private SlaveRepositoryManager srm;
	
	private StatusTransferOperation sto;

	public BatchFilesTransferOperation(BaseTransferOperations bto, 
									   FileTransferOperation fto, 
									   StatusTransferOperation sto, 
									   SlaveRepositoryManager srm,
									   FilesContextTransformer fct) 
	{
		super();
		this.fto = fto;
		this.bto = bto;
		this.fct = fct;
		this.srm = srm;
		this.sto = sto;
	}

	public void executeAsMaster(OutputStream os, PushbackInputStream pushbackInputStream) throws IOException {
		OperationType ot = null;
		while (REQUEST_BATCH_END != (ot=bto.checkOperationType(pushbackInputStream))) {
			if(ot == null) {
				// if connection is aborted
				// error detected
			}
			
			switch (ot) {
			case REQUEST_MASTER_STATUS_START: 
				sto.executeAsMaster(os, pushbackInputStream, MasterStatus.READY);
				break;
			case REQUEST_BATCH_START:
				bto.receiveOperationType(pushbackInputStream);
				logger.info("[" + this.getClass().getSimpleName() + "] slave requested batch transfer start operation");

				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_START);
				logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation accept");
				break;
			case REQUEST_FILE_START:
				fto.executeAsMaster(os, pushbackInputStream);
				break;
			default:
				// error detected
				break;
			}
		}
		//read REQUEST_BATCH_END byte
		bto.receiveOperationType(pushbackInputStream);
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested batch transfer end operation");

		bto.sendOperationType(os, OperationType.RESPONSE_BATCH_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer end operation accept");
	}

	public void executeAsSlave(OutputStream os, InputStream is) throws InterruptedException, IOException, MasterNotReadyDuringBatchTransfer {
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation request");

		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			// error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer start");

		//2. Get next file path for corresponding file need to be transfered and
		// send file transfer request to master
		srm.reset();
		while(srm.getStatus() != SlaveRepositoryManagerStatus.TERMINATED) {
			RepositoryRecord rr = srm.next();
			if (rr != null) {
				fto.executeAsSlave(os, is, fct.transform(rr));
			} else {
				// send status check message
				// must be READY any time but may be logged for testing purposes   
				MasterStatus status = sto.executeAsSlave(os, is);
				if(MasterStatus.READY != status) {
					throw new MasterNotReadyDuringBatchTransfer();
				}
				
				Thread.sleep(1000);
			}
		}
		
		// 3. Send end batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer end operation request");

		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_END) {
			// error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer end");
	}

}
