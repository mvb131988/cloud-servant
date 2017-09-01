package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import protocol.context.FileContext;
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
	public void executeAsMaster(OutputStream os, InputStream is, FilesContext fsc) {
		//1. Get start batch flag
		
		//for all files
		fto.executeAsMaster(os, is, fsc.next());
		
		//3. Get end batch flag
	}
	
	public void executeAsSlave(OutputStream os, InputStream is, FilesContext fsc) {
		//1. Send start batch flag
		
		String cyrilicName = "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u005f\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u043e\u0435\u005f\u043f\u043e\u005f\u0434\u043b\u0438\u043d\u0435\u005f\u0438\u043c\u044f";
		
		Path repositoryRoot = Paths.get("C:\\temp");
		Path relativePath = Paths.get(cyrilicName + ".jpg");
		long size = 0;
		long creationDateTime = 0;
		try {
			size = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).size();
			creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileContext fc = (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(relativePath)
				.setSize(size)
				.setCreationDateTime(creationDateTime)
				.build(); 
		
		//for all files
		fto.executeAsSlave(os, is, fsc.next());
		
		//3. Send end batch flag 
	}
	
}
