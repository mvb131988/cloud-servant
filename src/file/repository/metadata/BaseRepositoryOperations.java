package file.repository.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import protocol.file.FrameProcessor;

public class BaseRepositoryOperations {

	private Path repositoryRoot = Paths.get("C:\\temp");
	private final static int BATCH_SIZE = 10000;

	private FrameProcessor frameProcessor = new FrameProcessor();
	
	
	public BaseRepositoryOperations(FrameProcessor frameProcessor) {
		super();
		this.frameProcessor = frameProcessor;
	}

	public Set<String> readNames() {
		Set<String> names = new HashSet<>();
		for (RepositoryRecord rr : readAll()) {
			names.add(rr.getFileName());
		}
		return names;
	}
	
	//TODO: Optimize read. Don't read the entire file at a time. Instead provide a reader(inner class)
	// kind of iterator(buffered) to limit loaded data to buffer size 
	//
	public List<RepositoryRecord> readAll() {
		List<RepositoryRecord> records = new ArrayList<>();
		byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];

		Path configPath = repositoryRoot.resolve("data.repo");

		int bufSize = 0;
		try (InputStream is = Files.newInputStream(configPath);) {
			while ((bufSize = is.read(buffer)) != -1) {

				// build RepositoryRecord
				int offset = 0;
				while (offset != bufSize) {

					byte[] bId = new byte[RecordConstants.ID_SIZE];
					System.arraycopy(buffer, offset, bId, 0, RecordConstants.ID_SIZE);
					long id = frameProcessor.extractSize(bId);
					offset += RecordConstants.ID_SIZE;

					byte[] bSize = new byte[RecordConstants.NAME_LENGTH_SIZE];
					System.arraycopy(buffer, offset, bSize, 0, RecordConstants.NAME_LENGTH_SIZE);
					long length = frameProcessor.extractSize(bSize);
					offset += RecordConstants.NAME_LENGTH_SIZE;

					byte[] bFileName = new byte[(int) RecordConstants.NAME_SIZE];
					System.arraycopy(buffer, offset, bFileName, 0, (int) length);
					String fileName = new String(bFileName, 0, (int) length, "UTF-8");
					offset += RecordConstants.NAME_SIZE;

					byte status = buffer[offset];
					offset += RecordConstants.STATUS_SIZE;

					RepositoryRecord rr = new RepositoryRecord();
					rr.setId(id);
					rr.setFileameSize(length);
					rr.setFileName(fileName);
					rr.setStatus(status);
					records.add(rr);
				}

				buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return records;
	}
	
}
