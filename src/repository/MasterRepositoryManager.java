package repository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.FilePathMaxLengthException;
import main.AppProperties;

/**
 * Main class that scans the repository and creates data.repo
 * 
 * data.repo file - file with the list of all files saved in the repository. 
 */
public class MasterRepositoryManager {

	private final int smallTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private Path repositoryRoot;

	private RepositoryScaner repositoryScaner;
	
	private RepositoryVisitor repositoryVisitor;
	
	private BaseRepositoryOperations bro;

	public MasterRepositoryManager(RepositoryVisitor repositoryVisitor, BaseRepositoryOperations bro, AppProperties appProperties) {
		repositoryRoot = appProperties.getRepositoryRoot();
		
		this.bro = bro;
		this.repositoryVisitor = repositoryVisitor;
		this.smallTimeout = appProperties.getSmallPoolingTimeout();
	}
	
	/**
	 * Rescans the whole repository and recreates repository data.repo where the
	 * records corresponding to all files are.
	 * @throws IOException 
	 */
	private List<String> scan() throws IOException {
		repositoryVisitor.reset();
		Files.walkFileTree(repositoryRoot, repositoryVisitor);
		return repositoryVisitor.getFilesList();
	}

	/**
	 * Initializes Recreates data.repo file. Existed file is replaced by an
	 * empty one.
	 * @throws IOException 
	 */
	public void init() {
		try {
			// Create log directory
			Path log = Paths.get(".log");
			bro.createDirectoryIfNotExist(log);
			bro.hideDirectory(log);
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] unable to create log file", e);
		} 
		
		Path configPath = repositoryRoot.resolve("data.repo");
		try (OutputStream os = Files.newOutputStream(configPath);) {

		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] unable to create data.repo", e);
		} 
	}

	/**
	 * Writes the list of files into data.repo
	 * 
	 * @param fileNames
	 *            - list of files(relative names) to be written into data.repo
	 * @throws FilePathMaxLengthException 
	 * @throws IOException 
	 */
	private void writeAll(List<String> fileNames) throws IOException, FilePathMaxLengthException {
		bro.writeAll(fileNames, 0);
	}

	public Thread getScanerThread() {
		return new Thread(getScaner());
	}
	
	public RepositoryScaner getScaner() {
		if(repositoryScaner == null) {
			return new RepositoryScaner();
		}
		return repositoryScaner;
	}
	
	//TODO(FUTURE): Don't get all file names into the memory at once.
	//Instead implement (partial scan->flush) process
	public class RepositoryScaner implements Runnable {

		private RepositoryScannerStatus status;
		
		private RepositoryScaner() {
			status = RepositoryScannerStatus.READY;
		}
		
		@Override
		public void run() {
			try {
				for(;;) {
					if(status == RepositoryScannerStatus.BUSY) {
						logger.info("[" + this.getClass().getSimpleName() + "] scan started");
					
						writeAll(scan());
					
						status = RepositoryScannerStatus.READY;
						logger.info("[" + this.getClass().getSimpleName() + "] scan ended");
					}
					//Thread idle timeout.
					//Wait 1 second to avoid resources overconsumption.
					Thread.sleep(smallTimeout);
				}
			}
			catch (Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}

		public RepositoryScannerStatus getStatus() {
			return status;
		}
		
		public void reset() {
			status = RepositoryScannerStatus.BUSY;
		}
		
	}
	
}
