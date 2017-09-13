package transformer;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import file.repository.metadata.RecordConstants;
import file.repository.metadata.RepositoryRecord;
import protocol.context.FileContext;
import protocol.file.FrameProcessor;

public class FilesContextTransformer {

	private Path repositoryRoot = Paths.get("C:\\temp");
	
	private FrameProcessor frameProcessor;
	
	public FilesContextTransformer(FrameProcessor frameProcessor) {
		super();
		this.frameProcessor = frameProcessor;
	}

	public FileContext transform(RepositoryRecord rr) {
		return (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(Paths.get(rr.getFileName()))
				.build(); 
	}
	
	/**
	 * Requirement: bytes array is transformable(its structure exactly corresponds to structure of repository records) 
	 * into list of repository records. 
	 * 
	 * @param bytes
	 * @param bytesSize
	 * @return
	 */
	public List<RepositoryRecord> transform(byte[] bytes, int bytesSize) {
		List<RepositoryRecord> records = new ArrayList<>();
		
		// build RepositoryRecord
		int offset = 0;
		while (offset != bytesSize) {

			byte[] bId = new byte[RecordConstants.ID_SIZE];
			System.arraycopy(bytes, offset, bId, 0, RecordConstants.ID_SIZE);
			long id = frameProcessor.extractSize(bId);
			offset += RecordConstants.ID_SIZE;

			byte[] bSize = new byte[RecordConstants.NAME_LENGTH_SIZE];
			System.arraycopy(bytes, offset, bSize, 0, RecordConstants.NAME_LENGTH_SIZE);
			long length = frameProcessor.extractSize(bSize);
			offset += RecordConstants.NAME_LENGTH_SIZE;

			byte[] bFileName = new byte[(int) RecordConstants.NAME_SIZE];
			System.arraycopy(bytes, offset, bFileName, 0, (int) length);
			
			String fileName = null;
			try {
				fileName = new String(bFileName, 0, (int) length, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			offset += RecordConstants.NAME_SIZE;

			byte status = bytes[offset];
			offset += RecordConstants.STATUS_SIZE;

			RepositoryRecord rr = new RepositoryRecord();
			rr.setId(id);
			rr.setFileameSize(length);
			rr.setFileName(fileName);
			rr.setStatus(status);
			records.add(rr);
		}
		
		return records;
	}
	
}
