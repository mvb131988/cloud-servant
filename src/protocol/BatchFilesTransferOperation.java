package protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import protocol.constant.OperationType;
import protocol.context.FilesContext;

import static protocol.constant.OperationType.REQUEST_BATCH_END;

/**
 * Implements transfer protocol for a batch of files. Both master and slave
 * sides.
 */
public class BatchFilesTransferOperation {

	private FileTransferOperation fto;

	private BaseTransferOperations bto;

	public BatchFilesTransferOperation(FileTransferOperation fto, BaseTransferOperations bto) {
		super();
		this.fto = fto;
		this.bto = bto;
	}

	public void executeAsMaster(OutputStream os, InputStream is) {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(is);

		OperationType ot = null;
		while (REQUEST_BATCH_END != (ot=bto.checkOperationType(pushbackInputStream))) {
			if(ot == null) {
				// error detected
			}
			
			switch (ot) {
			case REQUEST_BATCH_START:
				bto.sendOperationType(os, OperationType.RESPONSE_BATCH_START);
				break;

			case REQUEST_BATCH_END:
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
	}

	public void executeAsSlave(OutputStream os, InputStream is, FilesContext fsc) {
		// 1. Send start batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_START);
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_START) {
			// error detected
		}

		// for all files
		fto.executeAsSlave(os, is, fsc.next());

		// 3. Send end batch flag
		bto.sendOperationType(os, OperationType.REQUEST_BATCH_END);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_BATCH_END) {
			// error detected
		}
	}

}