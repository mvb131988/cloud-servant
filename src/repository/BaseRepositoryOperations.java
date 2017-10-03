package repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import exception.FilePathMaxLengthException;
import main.AppProperties;
import repository.status.AsynchronySearcherStatus;
import transformer.FilesContextTransformer;
import transformer.LongTransformer;

public class BaseRepositoryOperations {

	private Path repositoryRoot;
	private final static int BATCH_SIZE = 10000;

	private LongTransformer frameProcessor;

	private FilesContextTransformer fct;

	public BaseRepositoryOperations(LongTransformer frameProcessor, FilesContextTransformer fct,
			AppProperties appProperties) {
		super();
		this.frameProcessor = frameProcessor;
		this.fct = fct;

		this.repositoryRoot = appProperties.getRepositoryRoot();
	}

	// --------------------------------------------------------------------------------------------------------------------------------
	// Set of unused methods which are suitable for investigation purpose
	// --------------------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	public long countRecords() throws Exception {
		long counter = -1;

		Path configPath = repositoryRoot.resolve("master.repo");
		long size = Files.readAttributes(configPath, BasicFileAttributes.class).size();
		counter = size / RecordConstants.FULL_SIZE;
		long remainder = size % RecordConstants.FULL_SIZE;

		// File is corrupted
		if (remainder != 0) {
			throw new Exception("File is corrupted");
		}

		return counter;
	}

	@SuppressWarnings("unused")
	public void write(int baseAddr, int id, String name, byte status) throws FileNotFoundException, IOException {
		Path configPath = repositoryRoot.resolve("master.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "rw")) {

			// offset = cursor pos
			int offset = baseAddr;

			// file id
			file.seek(offset);
			file.write(frameProcessor.packLong(id));
			offset += 8;

			// file name length
			long length = name.getBytes("UTF-8").length;
			file.write(frameProcessor.packLong(length));
			offset += 8;

			// Set maximum number of bytes for file name (200 bytes as example)
			file.write(name.getBytes("UTF-8"));
			offset += 200;

			file.seek(offset);
			file.write(status);
			offset += 1;

		}
	}

	@SuppressWarnings("unused")
	public RepositoryRecord read(int baseAddr) throws FileNotFoundException, IOException {
		RepositoryRecord repositoryRecord = new RepositoryRecord();

		int offset = baseAddr;

		Path configPath = repositoryRoot.resolve("data.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "r")) {
			// file id
			file.seek(offset);
			byte[] bId = new byte[8];
			file.read(bId, 0, 8);
			long id = frameProcessor.extractLong(bId);
			offset += 8;

			// file name length
			byte[] bLength = new byte[8];
			int l = file.read(bLength, 0, 8);
			long length = frameProcessor.extractLong(bLength);
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

		}

		return repositoryRecord;
	}

	// --------------------------------------------------------------------------------------------------------------------------------
	// Unused finished
	// --------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Writes the list of files into data.repo Much more faster than random
	 * access file
	 *
	 * Record format(defined by RecordConstants)-------------------------- | 8
	 * bytes| 8 bytes | 500 bytes| 1 byte |
	 * ------------------------------------------------------------------- |
	 * fileId | size of fileName | fileName | fileStatus |
	 * -------------------------------------------------------------------
	 *
	 * @param fileNames
	 *            - list of files(relative names) to be written into data.repo
	 * @param startId
	 *            - number used as starting to generate(increment sequence)
	 *            unique ids for all files from fileNames
	 * @throws IOException
	 * @throws FilePathMaxLengthException
	 */
	public void writeAll(List<String> fileNames, int startId) throws IOException, FilePathMaxLengthException {
		byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
		int offset = 0;

		int id = startId;

		Path configPath = repositoryRoot.resolve("data.repo");

		try (OutputStream os = Files.newOutputStream(configPath);) {

			// write record
			for (String fileName : fileNames) {

				// file id
				byte[] bSize = frameProcessor.packLong(++id);
				System.arraycopy(bSize, 0, buffer, offset, RecordConstants.ID_SIZE);
				offset += RecordConstants.ID_SIZE;

				// file name length
				long length = fileName.getBytes("UTF-8").length;
				bSize = frameProcessor.packLong(length);
				System.arraycopy(bSize, 0, buffer, offset, RecordConstants.NAME_LENGTH_SIZE);
				offset += RecordConstants.NAME_LENGTH_SIZE;

				// Set maximum number of bytes for file name
				// In the worst case file path could contain
				// RecordConstants.NAME_SIZE/2 cyrillic symbols(2 bytes per
				// cyrilic symbol)
				byte[] bFileName = fileName.getBytes("UTF-8");
				// TODO(MAJOR): Propagate higher when exception handling is
				// ready
				if (bFileName.length > RecordConstants.NAME_SIZE) {
					throw new FilePathMaxLengthException();
				}
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

	}

	/**
	 * Creates directory relative to the repository root
	 * 
	 * @throws IOException
	 */
	public void createDirectoryIfNotExist(Path relativePath) throws IOException {
		if (relativePath != null) {
			Path newPath = repositoryRoot.resolve(relativePath);
			if (!Files.exists(newPath)) {
				Files.createDirectories(newPath);
			}
		}
	}

	public long getSize(Path relativePath) throws IOException {
		long size = 0;
		size = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).size();
		return size;
	}

	public long getCreationDateTime(Path relativePath) throws IOException {
		long creationDateTime = 0;
		creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class)
								.creationTime().toMillis();
		return creationDateTime;
	}

	/**
	 * Assign hidden attribute to the directory
	 * @throws IOException 
	 */
	// TODO(NORMAL): os dependent operation. Execute only on windows. On linux
	// dir started with '.' is already hidden.
	public void hideDirectory(Path relativePath) throws IOException {
		Files.setAttribute(repositoryRoot.resolve(relativePath), "dos:hidden", Boolean.TRUE,
																			   LinkOption.NOFOLLOW_LINKS);
	}

	/**
	 * Move newly received file form temp directory to actual location
	 * 
	 * @param relativeFilePath
	 *            - file name with intermediate directories. When moved to
	 *            repository file will be saved in intermediate directories
	 * @param creationDateTime
	 * @throws IOException 
	 */
	public void fromTempToRepository(Path relativeFilePath, long creationDateTime) throws IOException {
		Path src = repositoryRoot.resolve(".temp").resolve(relativeFilePath.getFileName());
		Path dstn = repositoryRoot.resolve(relativeFilePath).normalize();
		Files.copy(src, dstn, StandardCopyOption.REPLACE_EXISTING);
		Files.delete(src);

		// If service crashes here creation date could be lost
		Files.setAttribute(dstn, "creationTime", FileTime.fromMillis(creationDateTime));
		Files.setAttribute(dstn, "lastModifiedTime", FileTime.fromMillis(creationDateTime));
		Files.setAttribute(dstn, "lastAccessTime", FileTime.fromMillis(creationDateTime));
	}

	public boolean existsFile(Path relativePath) {
		return Files.exists(repositoryRoot.resolve(relativePath));
	}

	/**
	 * Open data.repo main repository config and return input stream associated
	 * with it.
	 * @throws IOException 
	 */
	private InputStream openDataRepo() throws IOException {
		Path configPath = repositoryRoot.resolve("data.repo");
		InputStream is = null;
		is = Files.newInputStream(configPath);
		return is;
	}

	/**
	 * Open input stream associated with data.repo main repository config.
	 * @throws IOException 
	 */
	private void closeDataRepo(InputStream is) throws IOException {
		is.close();
	}

	/**
	 * Returns next bytes chunk(it contains exactly number of bytes
	 * corresponding to batch_size RepositoryRecords) from the input stream
	 * associated with data.repo main repository config.
	 * 
	 * Returns number of read bytes or -1 if end of stream reached
	 * @throws IOException 
	 */
	private int next(InputStream is, byte[] buffer) throws IOException {
		int readBytes = 0;
		readBytes = is.read(buffer);
		return readBytes;
	}

	/**
	 * asynchrony - is a file that exists in data.repo and doesn't in slave file
	 * system or vice versa.
	 */
	// TODO(FUTURE): Delete operation isn't implemented
	public class AsynchronySearcher implements Runnable {

		// Records that don't have corresponding file in the repository
		private Queue<RepositoryRecord> asynchronyBuffer;

		// max asynchrony buffer size
		private final int asynchronyBufferMaxSize = 500;

		private AsynchronySearcherStatus asynchronySearcherStatus;

		private Lock lock = new ReentrantLock();

		private AsynchronySearcher() {
			asynchronyBuffer = new ArrayBlockingQueue<>(asynchronyBufferMaxSize);
			asynchronySearcherStatus = AsynchronySearcherStatus.READY;
		}

		@Override
		public void run() {
			try{
				asynchronySearcherStatus = AsynchronySearcherStatus.BUSY;
	
				byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
	
				InputStream is = openDataRepo();
				int readBytes = 0;
				while ((readBytes = next(is, buffer)) != -1) {
					List<RepositoryRecord> records = fct.transform(buffer, readBytes);
					for (RepositoryRecord rr : records) {
	
						// Set current record path to be used for file transfer
						if (!existsFile(Paths.get(rr.getFileName()))) {
							// if previous read asynchrony isn't consumed wait until
							// it is consumed
							while (bufferFull()) {
								Thread.sleep(1000);
							}
							setAsynchrony(rr);
						}
	
						// If file for record exists skip it move to next one
						if (existsFile(Paths.get(rr.getFileName()))) {
							// log as existed one
						}
					}
				}
				closeDataRepo(is);
	
				// Last read. Wait until last read record isn't consumed.
				while (!bufferEmpty()) {
					Thread.sleep(1000);
				}
	
				asynchronySearcherStatus = AsynchronySearcherStatus.TERMINATED;
			} 
			catch(Exception e) {
				//TODO: Log exception
			}
		}

		/**
		 * @return true if buffer size exceeds asynchronyBufferMaxSize
		 */
		private boolean bufferFull() {
			return asynchronyBuffer.size() == asynchronyBufferMaxSize;
		}

		/**
		 * @return true when buffer size is zero
		 */
		private boolean bufferEmpty() {
			return asynchronyBuffer.size() == 0;
		}

		public RepositoryRecord nextAsynchrony() {
			return asynchronyBuffer.poll();
		}

		private void setAsynchrony(RepositoryRecord rr) {
			asynchronyBuffer.offer(rr);
		}

		public AsynchronySearcherStatus getStatus() {
			return asynchronySearcherStatus;
		}

	}

	public AsynchronySearcher getAsynchronySearcher() {
		return new AsynchronySearcher();
	}
}
