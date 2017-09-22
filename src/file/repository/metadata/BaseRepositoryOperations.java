package file.repository.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import file.repository.metadata.status.AsynchronySearcherStatus;
import main.AppProperties;
import protocol.file.FrameProcessor;
import transformer.FilesContextTransformer;

public class BaseRepositoryOperations {

	private Path repositoryRoot;
	private final static int BATCH_SIZE = 10000;

	private FrameProcessor frameProcessor;
	
	private FilesContextTransformer fct;
	
	public BaseRepositoryOperations(FrameProcessor frameProcessor, FilesContextTransformer fct, AppProperties appProperties) {
		super();
		this.frameProcessor = frameProcessor;
		this.fct = fct;
		
		this.repositoryRoot = appProperties.getRepositoryRoot();
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
	
	public long getSize(Path relativePath) {
		long size = 0;
		try {
			size = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).size();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}
	
	public long getCreationDateTime(Path relativePath) {
		long creationDateTime = 0;
		try {
			creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return creationDateTime;
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
		
		//Records that don't have corresponding file in the repository
		//TODO: replace to synchronized queue
		private Queue<RepositoryRecord> asynchronyBuffer;
		
		//max asynchrony buffer size
		private final int asynchronyBufferMaxSize = 100;
		
		private AsynchronySearcherStatus asynchronySearcherStatus;
		
		private Lock lock = new ReentrantLock();
		
		private AsynchronySearcher() { 
			asynchronyBuffer = new LinkedList<>();
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
					if(!existsFile(Paths.get(rr.getFileName())) && !bufferFull()) {
						setAsynchrony(rr);
					}
					//2. if previous read asynchrony isn't consumed wait until it is consumed
					else if(!existsFile(Paths.get(rr.getFileName())) && bufferFull()) {
						while(bufferFull()) {
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
			while(!bufferEmpty()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			asynchronySearcherStatus = AsynchronySearcherStatus.TERMINATED;
		}
		
		/**
		 * @return true if buffer size exceeds asynchronyBufferMaxSize
		 */
		private boolean bufferFull() {
			return asynchronyBuffer.size() > asynchronyBufferMaxSize;
		}
		
		/**
		 * @return true when buffer size is zero
		 */
		private boolean bufferEmpty() {
			return asynchronyBuffer.size() == 0;
		}
		
		public RepositoryRecord nextAsynchrony() {
			lock.lock();
			
			RepositoryRecord asynchrony = asynchronyBuffer.poll();
			
			lock.unlock();
			return asynchrony;
		}
		
		private void setAsynchrony(RepositoryRecord rr) {
			lock.lock();
			
			asynchronyBuffer.add(rr);
			
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
