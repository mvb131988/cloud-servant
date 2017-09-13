package protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.RepositoryRecord;
import protocol.constant.OperationType;
import protocol.context.EagerFilesContext;
import protocol.context.FilesContext;
import protocol.context.LazyFilesContext;
import transformer.FilesContextTransformer;

import static protocol.constant.OperationType.REQUEST_BATCH_END;

/**
 * Implements transfer protocol for a batch of files. Both master and slave
 * sides.
 */
@Deprecated
public class BatchFilesTransferOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private FileTransferOperation fto;

	private BaseTransferOperations bto;
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;

	public BatchFilesTransferOperation(FileTransferOperation fto, 
									   BaseTransferOperations bto, 
									   FilesContextTransformer fct, 
									   BaseRepositoryOperations bro) 
	{
		super();
		this.fto = fto;
		this.bto = bto;
		this.fct = fct;
		this.bro = bro;
	}

	public void executeAsMaster(OutputStream os, InputStream is) {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(is);

		OperationType ot = null;
		while (REQUEST_BATCH_END != (ot=bto.checkOperationType(pushbackInputStream))) {
			if(ot == null) {
				// if connection is aborted
				// error detected
			}
			
			switch (ot) {
			case REQUEST_BATCH_START:
				//read REQUEST_BATCH_START byte
				bto.receiveOperationType(pushbackInputStream);
				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_START);
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
	}

	public void executeAsSlave(OutputStream os, InputStream is, FilesContext fsc) {
//		// 1. Send start batch flag
//		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
//		OperationType ot = bto.receiveOperationType(is);
//		if (ot != OperationType.RESPONSE_BATCH_START) {
//			// error detected
//		}
//
//		while(fsc.hasNext()) {
//			fto.executeAsSlave(os, is, fsc.next());
//		}
//
//		// 3. Send end batch flag
//		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
//		ot = bto.receiveOperationType(is);
//		if (ot != OperationType.RESPONSE_BATCH_END) {
//			// error detected
//		}
		
		List<RepositoryRecord> records = bro.readAll();
		LazyFilesContext lfc = new LazyFilesContext(records, fct);

		// Batch operation
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation request");

		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			// error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer start");

		while (lfc.hasNext()) {
			fto.executeAsSlave(os, is, lfc.next());
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
