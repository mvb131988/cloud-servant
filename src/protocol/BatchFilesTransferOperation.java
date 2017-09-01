package protocol;

import java.io.InputStream;
import java.io.OutputStream;

import protocol.context.FilesContext;

/**
 *	Implements transfer protocol for a batch of files. Both master and slave sides. 
 */
public class BatchFilesTransferOperation {

	private FileTransferOperation fto;
	
	public BatchFilesTransferOperation(FileTransferOperation fto) {
		super();
		this.fto = fto;
	}

	//TODO: remove FilesContext
	public void executeAsMaster(OutputStream os, InputStream is) {
		//1. Get start batch flag
		
		//for all files
		fto.executeAsMaster(os, is);
		
		//3. Get end batch flag
	}
	
	public void executeAsSlave(OutputStream os, InputStream is, FilesContext fsc) {
		//1. Send start batch flag
		
		//for all files
		fto.executeAsSlave(os, is, fsc.next());
		
		//3. Send end batch flag 
	}
	
}
