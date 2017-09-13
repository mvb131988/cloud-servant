package protocol;

import static protocol.constant.OperationType.REQUEST_BATCH_END;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.RepositoryRecord;
import file.repository.metadata.SlaveRepositoryManager;
import file.repository.metadata.status.SlaveRepositoryManagerStatus;
import protocol.constant.OperationType;
import protocol.context.FilesContext;
import transformer.FilesContextTransformer;

/**
 * Implements transfer protocol for a batch of files. Both master and slave
 * sides.
 */
public class BatchFilesTransferOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private FileTransferOperation fto;

	private BaseTransferOperations bto;
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	private SlaveRepositoryManager srm;
	
	private StatusTransferOperation sto;

	public BatchFilesTransferOperation(FileTransferOperation fto, 
									   BaseTransferOperations bto, 
									   FilesContextTransformer fct, 
									   BaseRepositoryOperations bro,
									   SlaveRepositoryManager srm,
									   StatusTransferOperation sto) 
	{
		super();
		this.fto = fto;
		this.bto = bto;
		this.fct = fct;
		this.bro = bro;
		this.srm = srm;
		this.sto = sto;
	}

	@Deprecated
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
//		List<RepositoryRecord> records = bro.readAll();
//		LazyFilesContext lfc = new LazyFilesContext(records, fct);

		// Batch operation
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		logger.info("[" + this.getClass().getSimpleName() + "] sent batch transfer start operation request");

		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			// error detected
		}
		logger.info("[" + this.getClass().getSimpleName() + "] master responded batch transfer start");

//		while (lfc.hasNext()) {
//			fto.executeAsSlave(os, is, lfc.next());
//		}

		srm.reset();
		while(srm.getStatus() != SlaveRepositoryManagerStatus.TERMINATED) {
			RepositoryRecord rr = srm.next();
			if (rr != null) {
				fto.executeAsSlave(os, is, fct.transform(rr));
			} else {
				// send status check message
				// TODO: must be READY any time but may be logged for testing purposes   
				sto.executeAsSlave(os, is);
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
