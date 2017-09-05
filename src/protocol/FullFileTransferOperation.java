package protocol;

import static protocol.constant.OperationType.REQUEST_BATCH_END;
import static protocol.constant.OperationType.REQUEST_TRANSFER_START;
import static protocol.constant.OperationType.REQUEST_TRANSFER_END;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import file.repository.metadata.BaseRepositoryOperations;
import file.repository.metadata.RepositoryRecord;
import protocol.constant.OperationType;
import protocol.context.EagerFilesContext;
import protocol.context.FileContext;
import protocol.context.FilesContext;
import protocol.context.LazyFilesContext;
import transformer.FilesContextTransformer;

/**
 * Main operation of the protocol. Organize main protocol cycle, which handles all supported operations.  
 */
public class FullFileTransferOperation {

	private BaseTransferOperations bto;
	
	private FileTransferOperation fto;
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	public FullFileTransferOperation(FileTransferOperation fto, BaseTransferOperations bto, BaseRepositoryOperations bro, FilesContextTransformer fct) {
		super();
		this.bto = bto;
		this.fto = fto;
		this.bro = bro;
		this.fct = fct;
	}

	public void executeAsMaster(OutputStream os, InputStream is) {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(is);

		OperationType ot = null;
		while (REQUEST_TRANSFER_END != (ot=bto.checkOperationType(pushbackInputStream))) {
			if(ot == null) {
				// if connection is aborted
				// error detected
			}
			
			switch (ot) {
			case REQUEST_TRANSFER_START:
				bto.receiveOperationType(pushbackInputStream);
				bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_START);
				break;
			case REQUEST_BATCH_START:
				//read REQUEST_BATCH_START byte
				bto.receiveOperationType(pushbackInputStream);
				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_START);
				break;
			case REQUEST_BATCH_END:
				bto.receiveOperationType(pushbackInputStream);
				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_END);
				break;
			case REQUEST_FILE_START:
				fto.executeAsMaster(os, pushbackInputStream);
				break;
			default:
				// error detected
				break;
			}
		}
		//read REQUEST_TRANSFER_END byte
		bto.receiveOperationType(pushbackInputStream);
		bto.sendOperationType(os, OperationType.RESPONSE_TRANSFER_END);
	}
	
	public void executeAsSlave(OutputStream os, InputStream is, FilesContext fsc) {
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_START);
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_START) {
			// error detected
		}
		
		// Get data.repo
		// TODO: parameterize what is possible
		Path repositoryRoot = Paths.get("C:\\temp");
		Path relativePath = Paths.get("data.repo");
		FileContext fc = (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(relativePath)
				.build(); 
		fto.executeAsSlave(os, is, fc);
		
		List<RepositoryRecord> records = bro.readAll();
		LazyFilesContext lfc = new LazyFilesContext(records, fct);
		
		// --- TODO: move to batch operation ---
		// Batch operation 
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			// error detected
		}

		while (lfc.hasNext()) {
			fto.executeAsSlave(os, is, lfc.next());
		}

		// 3. Send end batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_END) {
			// error detected
		}
		// --- move to batch operation ---
		
		bto.sendOperationType(os, OperationType.REQUEST_TRANSFER_END);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_TRANSFER_END) {
			// error detected
		}
	}
	
}
