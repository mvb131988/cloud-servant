package transfer;

import static transfer.constant.OperationType.REQUEST_BATCH_END;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.BatchFileTransferException;
import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
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

	private final int smallTimeout;
	
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
									   FilesContextTransformer fct,
									   AppProperties appProperties) 
	{
		super();
		this.fto = fto;
		this.bto = bto;
		this.fct = fct;
		this.srm = srm;
		this.sto = sto;
		this.smallTimeout = appProperties.getSmallPoolingTimeout();
	}

	public void executeAsMaster(OutputStream os, PushbackInputStream pushbackInputStream) throws IOException, WrongOperationException {
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
				throw new WrongOperationException();
			}
		}
		//read REQUEST_BATCH_END byte
		bto.receiveOperationType(pushbackInputStream);
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested batch transfer end operation");

		bto.sendOperationType(os, OperationType.RESPONSE_BATCH_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer end operation accept");
	}

	public void executeAsSlave(OutputStream os, InputStream is, String memberId) 
			throws InterruptedException, 
				   IOException, 
				   MasterNotReadyDuringBatchTransfer, 
				   WrongOperationException, 
				   BatchFileTransferException 
	{
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation request");

		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_BATCH_START + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer start");

		//2. Get next file path(for corresponding file) that is needed to be transfered and
		// send file transfer request to master
		try {
			srm.reset(memberId);
			while(srm.getStatus() != SlaveRepositoryManagerStatus.TERMINATED) {
				RepositoryRecord rr = srm.next();
				if (rr != null) {
					fto.executeAsSlave(os, is, fct.transform(rr));
				} else {
					// send status check message
					// must be READY at any time  
					MasterStatus status = sto.executeAsSlave(os, is).getMasterStatus();
					if(MasterStatus.READY != status) {
						throw new MasterNotReadyDuringBatchTransfer();
					}
					
					//If all records from the buffer of AsynchronySearcher are consumed and buffer is empty,
					//but AsynchronySearcher isn't terminated wait until it adds new records to the buffer.
					//Wait 1 second to avoid resources overconsumption.
					Thread.sleep(smallTimeout);
				}
			}
		}
		catch(Exception e) {
			//Catch IOException, that happened during file transfer and terminate SlaveRepositoryManager(asynchrony thread)
			while(srm.getStatus() != SlaveRepositoryManagerStatus.TERMINATED) {
				//Read all. When no more records available asynchrony thread terminates
				srm.next();
			}
			throw new BatchFileTransferException();
		}
		
		// 3. Send end batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer end operation request");

		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_BATCH_END + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer end");
	}

}
