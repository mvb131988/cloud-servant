package protocol;

import static protocol.constant.OperationType.REQUEST_TRANSFER_END;
import static protocol.constant.OperationType.REQUEST_TRANSFER_START;
import static protocol.constant.OperationType.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.RepositoryRecord;
import protocol.constant.MasterStatus;
import protocol.constant.OperationType;
import protocol.context.FileContext;
import protocol.context.FilesContext;
import protocol.context.LazyFilesContext;
import transformer.FilesContextTransformer;

/**
 * Main operation of the protocol. Organize main protocol cycle, which handles all supported operations.  
 * This operation is non-interruptible. MasterTransferManager must wait until it completely finishes.
 */
public class FullFileTransferOperation {
	
	private Logger logger = LogManager.getRootLogger();

	private BaseTransferOperations bto;
	
	private FileTransferOperation fto;
	
	private StatusTransferOperation sto;
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	private BatchFilesTransferOperation bfto;
	
	public FullFileTransferOperation(FileTransferOperation fto, 
									 BaseTransferOperations bto, 
									 BaseRepositoryOperations bro, 
									 FilesContextTransformer fct, 
									 StatusTransferOperation sto,
									 BatchFilesTransferOperation bfto) {
		super();
		this.bto = bto;
		this.fto = fto;
		this.bro = bro;
		this.fct = fct;
		this.sto = sto;
		this.bfto = bfto;
	}

	public void executeAsMaster(OutputStream os, InputStream is) {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(is);

		OperationType ot = null;
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
				bto.receiveOperationType(pushbackInputStream);
				logger.info("[" + this.getClass().getSimpleName() + "] slave requested batch transfer start operation");

				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_START);
				logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation accept");
				break;
			case REQUEST_BATCH_END:
				bto.receiveOperationType(pushbackInputStream);
				logger.info("[" + this.getClass().getSimpleName() + "] slave requested batch transfer end operation");

				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_END);
				logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer end operation accept");
				break;
			case REQUEST_FILE_START:
				fto.executeAsMaster(os, pushbackInputStream);
				break;
			default:
				// error detected
				break;
			}
		}
		// read REQUEST_TRANSFER_END byte
		bto.receiveOperationType(pushbackInputStream);
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested transfer end operation");

		bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer end operation accept");
	}
	
	public void executeAsSlave(OutputStream os, InputStream is, FilesContext fsc) {
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer start operation request");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_START) {
			// error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded transfer start");
		
		// Get data.repo
		// TODO: parameterize what is possible
		Path repositoryRoot = Paths.get("C:\\temp");
		Path relativePath = Paths.get("data.repo");
		FileContext fc = (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(relativePath)
				.build(); 
		fto.executeAsSlave(os, is, fc);
		logger.info("[" + this.getClass().getSimpleName() + "] data.repo received");
		
		List<RepositoryRecord> records = bro.readAll();
		LazyFilesContext lfc = new LazyFilesContext(records, fct);
		
		// batch transfer
		bfto.executeAsSlave(os, is, null);
		
		// --- TODO: move to batch operation ---
		// Batch operation 
		// 1. Send start batch flag
//		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
//		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation request");
//		
//		ot = bto.receiveOperationType(is);
//		if (ot != OperationType.RESPONSE_BATCH_START) {
//			// error detected
//		}
//		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer start");
//
//		while (lfc.hasNext()) {
//			fto.executeAsSlave(os, is, lfc.next());
//		}
//
//		// 3. Send end batch flag
//		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
//		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer end operation request");
//		
//		ot = bto.receiveOperationType(is);
//		if (ot != OperationType.RESPONSE_BATCH_END) {
//			// error detected
//		}
//		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer end");
		// --- move to batch operation ---
		
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_END);
		logger.info("[" + this.getClass().getSimpleName() + "] sent transfer end request");
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_END) {
			// error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded transfer end");
	}
	
}
