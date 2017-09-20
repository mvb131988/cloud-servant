package file.repository.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import file.repository.metadata.status.AsynchronySearcherStatus;
import protocol.file.FrameProcessor;
import transformer.FilesContextTransformer;

public class BaseRepositoryOperations {

	private Path repositoryRoot = Paths.get("C:\\temp");
	private final static int BATCH_SIZE = 10000;

	private FrameProcessor frameProcessor;
	
	private FilesContextTransformer fct;
	
	public BaseRepositoryOperations(FrameProcessor frameProcessor, FilesContextTransformer fct) {
		super();
		this.frameProcessor = frameProcessor;
		this.fct = fct;
	}

	public Set<String> readNames() {
		Set<String> names = new HashSet<>();
		for (RepositoryRecord rr : readAll()) {
			names.add(rr.getFileName());
		}
		return names;
	}
	
	/**
	 * Creates directory relative to the repository root 
	 */
	public void createDirectoryIfNotExist(Path relativePath) {
		if (relativePath != null) {
			try {
				Path newPath = repositoryRoot.resolve(relativePath);
				if (!Files.exists(newPath)) {
					Files.createDirectories(newPath);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Assign hidden attribute to the directory 
	 */
	//TODO: os dependent operation. Execute only on windows. On linux dir started with '.' is already hidden. 
	public void hideDirectory(Path relativePath) {
		try {
			Files.setAttribute(repositoryRoot.resolve(relativePath), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Move newly received file form temp directory to actual location
	 * 
	 * @param relativeFilePath - file name with intermediate directories. When moved to repository file will be saved in intermediate directories
	 * @param creationDateTime
	 */
	public void fromTempToRepository(Path relativeFilePath, long creationDateTime) {
		try {
			Path src = repositoryRoot.resolve(".temp").resolve(relativeFilePath.getFileName());
			Path dstn = repositoryRoot.resolve(relativeFilePath).normalize();
			Files.copy(src, dstn, StandardCopyOption.REPLACE_EXISTING);
			Files.delete(src);
			
			//If service crashes here creation date could be lost
			Files.setAttribute(dstn, "creationTime", FileTime.fromMillis(creationDateTime));
			Files.setAttribute(dstn, "lastModifiedTime", FileTime.fromMillis(creationDateTime));
			Files.setAttribute(dstn, "lastAccessTime", FileTime.fromMillis(creationDateTime));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//TODO: Optimize read. Don't read the entire file at a time. Instead provide a reader(inner class)
	// kind of iterator(buffered) to limit loaded data to buffer size 
	//
	@Deprecated
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
	
	public boolean existsFile(Path relativePath) {
		return Files.exists(repositoryRoot.resolve(relativePath));
	}
	
	/**
	 * Open data.repo main repository config and return input stream associated with it.   
	 */
	private InputStream openDataRepo() {
		Path configPath = repositoryRoot.resolve("data.repo");
		InputStream is = null;
		try {
			is = Files.newInputStream(configPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is;
	}
	
	/**
	 * Open input stream associated with data.repo main repository config.   
	 */
	private void closeDataRepo(InputStream is) {
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns next bytes chunk(it contains exactly number of bytes corresponding to batch_size RepositoryRecords) 
	 * from the input stream associated with data.repo main repository config. 
	 * 
	 * Returns number of read bytes or -1 if end of stream reached
	 */
	private int next(InputStream is, byte[] buffer) {
		int readBytes = 0;
		try {
			readBytes = is.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return readBytes;
	}
	
	
	/**
	 * asynchrony - is a file that exists in data.repo and doesn't in slave file system
	 * 				or vice versa.
	 */
	//TODO: Delete operation isn't implemented
	public class AsynchronySearcher implements Runnable {
		
		//TODO: improve by adding buffer queue
		private RepositoryRecord asynchrony;
		
		private AsynchronySearcherStatus asynchronySearcherStatus;
		
		private Lock lock = new ReentrantLock();
		
		private AsynchronySearcher() { 
			asynchrony = null;
			asynchronySearcherStatus = AsynchronySearcherStatus.READY;
		}
		
		@Override
		public void run() {
			asynchronySearcherStatus = AsynchronySearcherStatus.BUSY;
			
			byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];

			InputStream is = openDataRepo();
			int readBytes = 0;
			while((readBytes = next(is, buffer)) != -1){
				List<RepositoryRecord> records = fct.transform(buffer, readBytes);
				for(RepositoryRecord rr: records) {
					//1. Set current record path to be used for file transfer
					if(!existsFile(Paths.get(rr.getFileName())) && asynchrony == null) {
						setAsynchrony(rr);
					}
					//2. if previous read asynchrony isn't consumed wait until it is consumed
					else if(!existsFile(Paths.get(rr.getFileName())) && asynchrony != null) {
						while(asynchrony != null) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						setAsynchrony(rr);
					}
					//3. If file for record exists skip it move to next one
					if(existsFile(Paths.get(rr.getFileName()))) {
						// log as existed one
					}
				}
			}
			closeDataRepo(is);
			
			//Last read. Wait until last read record isn't consumed.
			while(asynchrony != null) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			asynchronySearcherStatus = AsynchronySearcherStatus.TERMINATED;
		}

		public RepositoryRecord nextAsynchrony() {
			lock.lock();
			
			RepositoryRecord copy = asynchrony;
			asynchrony = null;
			
			lock.unlock();
			return copy;
		}
		
		private void setAsynchrony(RepositoryRecord rr) {
			lock.lock();
			asynchrony = rr;
			lock.unlock();
		}
		
		public AsynchronySearcherStatus getStatus() {
			return asynchronySearcherStatus;
		}
		
	}
	
	public AsynchronySearcher getAsynchronySearcher() {
		return new AsynchronySearcher();
	}
}
