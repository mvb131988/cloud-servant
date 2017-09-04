package file.repository.metadata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import protocol.file.FrameProcessor;

/**
 * Main class that scans the repository and creates data.repo
 * 
 * data.repo file - file with the list of all files saved in the repository. 
 */
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

//TODO: check and move basic methods to BaseRepositoryOperations 
public class RepositoryManager {

	private Path repositoryRoot = Paths.get("D:\\temp");

	private FrameProcessor frameProcessor = new FrameProcessor();

	private final static int BATCH_SIZE = 10000;

	private byte[] internalBuffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];

	public static void main(String[] args) {
		RepositoryManager repositoryManager = new RepositoryManager();

		// List<RepositoryRecord> records = repositoryManager.readAll();

		// repositoryManager.loadRepositoryRecords(repositoryManager.countRecords());

		// repositoryManager.init();

		List<String> fileNames = repositoryManager.scan();
		repositoryManager.writeAll(fileNames, 0);
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
	 * Rescans the whole repository and recreates repository data.repo where the
	 * records corresponding to all files are.
	 */
	private List<String> scan() {
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
	 * Initializes Recreates data.repo file. Existed file is replaced by an
	 * empty one.
	 */
	private void init() {
		Path configPath = repositoryRoot.resolve("data.repo");
		try (OutputStream os = Files.newOutputStream(configPath);) {

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Writes the list of files into data.repo
	 * 
	 * @param fileNames
	 *            - list of files(relative names) to be written into data.repo
	 */
	private void writeAll(List<String> fileNames) {
		writeAll(fileNames, 0);
	}

	/**
	 * Writes the list of files into data.repo Much more faster than random
	 * access file
	 *
	 * @param fileNames
	 *            - list of files(relative names) to be written into data.repo
	 * @param startId
	 *            - number used as starting to generate(increment sequence)
	 *            unique ids for all files from fileNames
	 */
	public void writeAll(List<String> fileNames, int startId) {
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
	
	public Thread getScaner() {
		return new Thread(new RepositoryScaner());
	}
	
	private class RepositoryScaner implements Runnable {

		@Override
		public void run() {
			init();
			writeAll(scan());
		}
		
	}
	
}
