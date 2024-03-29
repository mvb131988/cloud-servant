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
import exception.OutboundMemberNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import repository.BaseRepositoryOperations;
import repository.status.RepositoryFileStatus;
import transfer.constant.MemberStatus;
import transfer.constant.OperationType;
import transfer.context.FileContext;

/**
 * Main operation of the protocol. Organize main protocol cycle, which handles all supported
 * operations. This operation is non-interruptible. Inbound/Outbound transfer managers from
 * both sides are busy until it's not entirely completed or exception during data transferring
 * occurs.
 */
public class FullFileTransferOperation {
	
	private Logger logger = LogManager.getRootLogger();

	private BaseTransferOperations bto;
	
	private BaseRepositoryOperations bro;
	
	private FileTransferOperation fto;
	
	private BatchFilesTransferOperation bfto;
	
	private HealthCheckOperation hco;
	
	public FullFileTransferOperation(BaseTransferOperations bto, 
									 BaseRepositoryOperations bro,
									 FileTransferOperation fto, 
									 HealthCheckOperation hco,
									 BatchFilesTransferOperation bfto,
									 AppProperties appProperties) 
	{
		super();
		this.bto = bto;
		this.bro = bro;
		this.fto = fto;
		this.bfto = bfto;
		this.hco = hco;
	}

	public void inbound(OutputStream os, PushbackInputStream pushbackInputStream) 
			throws IOException, WrongOperationException 
	{
		OperationType ot = null;
		while (REQUEST_TRANSFER_END != (ot = bto.checkOperationType(pushbackInputStream))) {
			if (ot == null) {
				// if connection is aborted
				// error detected
			}

			switch (ot) {
				case REQUEST_HEALTHCHECK_START: 
					hco.inbound(os, pushbackInputStream, MemberStatus.READY);
					break;
				case REQUEST_TRANSFER_START:
					bto.receiveOperationType(pushbackInputStream);
					logger.info("[" + this.getClass().getSimpleName() + "] inbound member "
							  + "requested transfer start operation");
	
					bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_START);
					logger.info("[" + this.getClass().getSimpleName() + "] sent transfer start "
							  + "operation accept");
					break;
				case REQUEST_BATCH_START:
					logger.info("[" + this.getClass().getSimpleName() 
							  + "] batch transfer started");
					bfto.inbound(os, pushbackInputStream);
					logger.info("[" + this.getClass().getSimpleName() + "] batch transfer ended");
					break;
				case REQUEST_FILE_START:
					logger.info("[" + this.getClass().getSimpleName() + "] file transfer started");
					fto.inbound(os, pushbackInputStream);
					logger.info("[" + this.getClass().getSimpleName() + "] file transfer ended");
					break;
				default:
					throw new WrongOperationException();
			}
		}
		// read REQUEST_TRANSFER_END byte
		bto.receiveOperationType(pushbackInputStream);
		logger.info("[" + this.getClass().getSimpleName() + "] inbound member requested transfer "
				  + "end operation");

		bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer end operation "
				  + "accept");
	}
	
	public void outbound(OutputStream os, InputStream is, String memberId) 
			throws InterruptedException, 
				   IOException, 
				   OutboundMemberNotReadyDuringBatchTransfer, 
				   WrongOperationException, 
				   BatchFileTransferException 
	{
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer start "
				  + "operation request");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_START) {
			throw new WrongOperationException(
					"Expected: " + OperationType.RESPONSE_TRANSFER_START + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() + "] outbound member responded "
				  + "transfer start");
		
		// Get data.repo
		Path relativePath = Paths.get(memberId + "_data.repo");
		FileContext fc = (new FileContext.Builder()).setRelativePath(relativePath).build();
		fto.outbound(os, is, fc);
		bro.updateDataRepoStatus(RepositoryFileStatus.RECEIVE_END, memberId);
		
		// batch transfer
		bfto.outbound(os, is, memberId);
		
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer end request");
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_END) {
			throw new WrongOperationException(
					"Expected: " + OperationType.RESPONSE_TRANSFER_END + " Actual: " + ot);
		}
		logger.info("[" + this.getClass().getSimpleName() + "] outbound member responded "
				  + "transfer end");
	}
	
}
