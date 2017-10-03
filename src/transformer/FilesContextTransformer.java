package transformer;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import repository.RecordConstants;
import repository.RepositoryRecord;
import transfer.context.FileContext;

public class FilesContextTransformer {

	private LongTransformer frameProcessor;
	
	public FilesContextTransformer(LongTransformer frameProcessor) {
		super();
		this.frameProcessor = frameProcessor;
	}

	public FileContext transform(RepositoryRecord rr) {
		return (new FileContext.Builder())
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
	 * @throws UnsupportedEncodingException 
	 */
	public List<RepositoryRecord> transform(byte[] bytes, int bytesSize) throws UnsupportedEncodingException {
		List<RepositoryRecord> records = new ArrayList<>();
		
		// build RepositoryRecord
		int offset = 0;
		while (offset != bytesSize) {

			byte[] bId = new byte[RecordConstants.ID_SIZE];
			System.arraycopy(bytes, offset, bId, 0, RecordConstants.ID_SIZE);
			long id = frameProcessor.extractLong(bId);
			offset += RecordConstants.ID_SIZE;

			byte[] bSize = new byte[RecordConstants.NAME_LENGTH_SIZE];
			System.arraycopy(bytes, offset, bSize, 0, RecordConstants.NAME_LENGTH_SIZE);
			long length = frameProcessor.extractLong(bSize);
			offset += RecordConstants.NAME_LENGTH_SIZE;

			byte[] bFileName = new byte[(int) RecordConstants.NAME_SIZE];
			System.arraycopy(bytes, offset, bFileName, 0, (int) length);
			
			String fileName = new String(bFileName, 0, (int) length, "UTF-8");
			
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
