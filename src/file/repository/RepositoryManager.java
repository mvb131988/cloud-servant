package file.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import protocol.file.FrameProcessor;

// See MappedByteBuffer to improve performance
/**
 * 
 * Record format ----------------------------------------------------- | 8
 * bytes| 8 bytes | 200 bytes| 1 byte |
 * ---------------------------------------------------- | fileId | size of
 * fileName | fileName | fileStatus |
 * -----------------------------------------------------
 *
 */
public class RepositoryManager {

	private Path repositoryRoot = Paths.get("D:\\Programs");

	private FrameProcessor frameProcessor = new FrameProcessor();

	private final static int BATCH_SIZE = 10000;
	
	private byte[] internalBuffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];

	public static void main(String[] args) {
		RepositoryManager repositoryManager = new RepositoryManager();

//		List<RepositoryRecord> records = repositoryManager.readAll();

		// repositoryManager.loadRepositoryRecords(repositoryManager.countRecords());

		// repositoryManager.init();

		 List<String> fileNames = repositoryManager.synchronizeRepository();
		 repositoryManager.write(fileNames, 0);
		//
		// int base = 0;
		// int recordSize = 8+8+200+1;
		// int id = 0;
		// for(String name: fileNames) {
		// repositoryManager.write(base, ++id, name, (byte)10);
		// base += recordSize;
		// }
		//
		// RepositoryRecord repoRecord =
		RepositoryRecord rr = repositoryManager.read(100003 * RecordConstants.FULL_SIZE);
		//
		System.out.println("Done");

		// repositoryManager.read(base2);
	}

	@SuppressWarnings("unused")
	public long countRecords() {
		long counter = -1;

		Path configPath = repositoryRoot.resolve("master.repo");
		try {
			long size = Files.readAttributes(configPath, BasicFileAttributes.class).size();
			counter = size / RecordConstants.FULL_SIZE;
			long remainder = size % RecordConstants.FULL_SIZE;

			// File is corrupted
			if (remainder != 0) {
				throw new Exception("File is corrupted");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return counter;
	}

	/**
	 * Loads all repository records into a map from data.repo
	 */
	public Map<String, RepositoryRecord> loadRepositoryRecords(long recordsCount) {
		Map<String, RepositoryRecord> names = new HashMap<>();

		int baseAddr = 0;
		for (int i = 0; i < recordsCount; i++) {
			RepositoryRecord rr = this.read(baseAddr);
			names.put(rr.getFileName(), rr);
			baseAddr += RecordConstants.FULL_SIZE;
		}

		return names;
	}

	/**
	 * Rescans the whole repository and recreate repository data.repo where the
	 * records corresponding to all files are.
	 */
	public List<String> synchronizeRepository() {
		RepositoryVisitor repoVisitor = new RepositoryVisitor();
		try {
			Files.walkFileTree(repositoryRoot, repoVisitor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return repoVisitor.getFilesList();
	}

	/**
	 * Recreates data.repo file. Existed file is replaced by an empty one.
	 */
	public void init() {
		Path configPath = repositoryRoot.resolve("master.repo");
		try (OutputStream os = Files.newOutputStream(configPath);) {

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Much more faster than random access file
	 */
	public void write(List<String> fileNames, int startId) {
		byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
		int offset = 0;

		int id = startId;

		Path configPath = repositoryRoot.resolve("data.repo");

		try (OutputStream os = Files.newOutputStream(configPath);) {

			// write record
			for (String fileName : fileNames) {

				// file id
				byte[] bSize = frameProcessor.packSize(++id);
				System.arraycopy(bSize, 0, buffer, offset, RecordConstants.ID_SIZE);
				offset += RecordConstants.ID_SIZE;

				// file name length
				long length = fileName.getBytes("UTF-8").length;
				bSize = frameProcessor.packSize(length);
				System.arraycopy(bSize, 0, buffer, offset, RecordConstants.NAME_LENGTH_SIZE);
				offset += RecordConstants.NAME_LENGTH_SIZE;

				// Set maximum number of bytes for file name (200 bytes as an
				// example)
				byte[] bFileName = fileName.getBytes("UTF-8");
				System.arraycopy(bFileName, 0, buffer, offset, (int) length);
				offset += RecordConstants.NAME_SIZE;

				// Set record status code
				byte status = 1;
				buffer[offset] = status;
				offset += RecordConstants.STATUS_SIZE;

				if (offset == buffer.length) {

					// flush full buffer
					os.write(buffer, 0, offset);
					os.flush();

					buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
					offset = 0;
				}
			}

			// final flush
			// from 0 to offset - 1
			os.write(buffer, 0, offset);
			os.flush();
		}

		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Set<String> readNames() {
		Set<String> names = new HashSet<>();
		for(RepositoryRecord rr: readAll()) {
			names.add(rr.getFileName());
		}
		return names;
	}
	
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

	@Deprecated
	public void write(int baseAddr, int id, String name, byte status) {
		Path configPath = repositoryRoot.resolve("master.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "rw")) {

			// offset = cursor pos
			int offset = baseAddr;

			// file id
			file.seek(offset);
			file.write(frameProcessor.packSize(id));
			offset += 8;

			// file name length
			long length = name.getBytes("UTF-8").length;
			file.write(frameProcessor.packSize(length));
			offset += 8;

			// Set maximum number of bytes for file name (200 bytes as example)
			file.write(name.getBytes("UTF-8"));
			offset += 200;

			file.seek(offset);
			file.write(status);
			offset += 1;

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	@Deprecated
	public RepositoryRecord read(int baseAddr) {
		RepositoryRecord repositoryRecord = new RepositoryRecord();

		int offset = baseAddr;

		Path configPath = repositoryRoot.resolve("data.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "r")) {
			// file id
			file.seek(offset);
			byte[] bId = new byte[8];
			file.read(bId, 0, 8);
			long id = frameProcessor.extractSize(bId);
			offset += 8;

			// file name length
			byte[] bLength = new byte[8];
			int l = file.read(bLength, 0, 8);
			long length = frameProcessor.extractSize(bLength);
			offset += 8;

			byte[] bName = new byte[200];
			file.read(bName, 0, 200);
			String name = new String(bName, 0, (int) length, "UTF-8");
			offset += 200;

			int status = file.read();
			offset++;

			repositoryRecord.setId(id);
			repositoryRecord.setFileName(name);
			repositoryRecord.setFileameSize(length);
			repositoryRecord.setStatus((byte) status);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return repositoryRecord;
	}

}
