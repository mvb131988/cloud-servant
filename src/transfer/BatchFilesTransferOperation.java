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
import repository.AsynchronySearcherManager;
import repository.RepositoryConsistencyChecker;
import repository.RepositoryRecord;
import repository.status.SlaveRepositoryManagerStatus;
import transfer.constant.MemberStatus;
import transfer.constant.OperationType;
import transformer.FilesContextTransformer;

/**
 * Implements transfer protocol for a batch of files. Both master and slave sides.
 */
public class BatchFilesTransferOperation {

	private final int smallTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private FileTransferOperation fto;

	private BaseTransferOperations bto;
	
	private FilesContextTransformer fct;
	
	private StatusTransferOperation sto;

	private AsynchronySearcherManager asm;
	
	private RepositoryConsistencyChecker rcc;
	
	public BatchFilesTransferOperation(BaseTransferOperations bto, 
									   FileTransferOperation fto, 
									   StatusTransferOperation sto, 
									   FilesContextTransformer fct,
									   AsynchronySearcherManager asm,
									   RepositoryConsistencyChecker rcc,
									   AppProperties appProperties) 
	{
		super();
		this.fto = fto;
		this.bto = bto;
		this.fct = fct;
		this.sto = sto;
		this.asm = asm;
		this.rcc = rcc;
		this.smallTimeout = appProperties.getSmallPoolingTimeout();
	}

	public void inbound(OutputStream os, PushbackInputStream pushbackInputStream)
			throws IOException, WrongOperationException 
	{
		OperationType ot = null;
		while (REQUEST_BATCH_END != (ot=bto.checkOperationType(pushbackInputStream))) {
			if(ot == null) {
				// if connection is aborted
				// error detected
			}
			
			switch (ot) {
				case REQUEST_MASTER_STATUS_START: 
					sto.executeAsMaster(os, pushbackInputStream, MemberStatus.READY);
					break;
				case REQUEST_BATCH_START:
					bto.receiveOperationType(pushbackInputStream);
					logger.info("[" + this.getClass().getSimpleName() 
							  + "] slave requested batch transfer start operation");
	
					bto.sendOperationType(os, OperationType.RESPONSE_BATCH_START);
					logger.info("[" + this.getClass().getSimpleName() 
							  + "] sent batch transfer start operation accept");
					break;
				case REQUEST_FILE_START:
					fto.inbound(os, pushbackInputStream);
					break;
				default:
					throw new WrongOperationException();
			}
		}
		//read REQUEST_BATCH_END byte
		bto.receiveOperationType(pushbackInputStream);
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] inbound member requested batch transfer end operation");

		bto.sendOperationType(os, OperationType.RESPONSE_BATCH_END);
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] sent batch transfer end operation accept");
	}

	public void outbound(OutputStream os, InputStream is, String memberId) 
			throws InterruptedException, 
				   IOException, 
				   MasterNotReadyDuringBatchTransfer, 
				   WrongOperationException, 
				   BatchFileTransferException 
	{
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] sent batch transfer start operation request");

		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_BATCH_START
					                        + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] outbound member responded batch transfer start");

		// 2. Get next file path (for corresponding file) that is needed to be transferred and
		// send file transfer request to master
		try {
			asm.startRepoAsyncSearcherThread(memberId);
			while(asm.repoAsyncSearcherThreadStatus() != SlaveRepositoryManagerStatus.TERMINATED) {
				RepositoryRecord rr = asm.next();
				if (rr != null) {
					fto.outbound(os, is, fct.transform(rr));
				} else {
					// TODO: send healthcheck here instead, necessary to avoid connection timeout
					// when scan takes too long
					
					// send status check message
					// must be READY at any time  
					MemberStatus status = sto.executeAsSlave(os, is).getOutboundMemberStatus();
					if(MemberStatus.READY != status) {
						throw new MasterNotReadyDuringBatchTransfer();
					}
					
					//If all records from the buffer of AsynchronySearcher are consumed and
					//buffer is empty, but AsynchronySearcher isn't terminated wait until it adds
					//new records to the buffer. Wait 1 second to avoid resources overconsumption.
					Thread.sleep(smallTimeout);
				}
			}
			//consistency check
			rcc.checkScan(memberId);
		}
		catch(Exception e) {
			//Catch IOException, that happened during file transfer and terminate 
			//SlaveRepositoryManager (asynchrony thread)
			while(asm.repoAsyncSearcherThreadStatus() != SlaveRepositoryManagerStatus.TERMINATED) {
				//Read all. When no more records available asynchrony thread terminates
				asm.next();
			}
			throw new BatchFileTransferException();
		}
		
		// 3. Send end batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] sent batch transfer end operation request");

		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_BATCH_END 
					                        + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] outbound member responded batch transfer end");
	}

}
