package repository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;

/**
 * Main class that scans the repository and creates data.repo
 * 
 * data.repo file - file with the list of all files saved in the repository. 
 */
public class MasterRepositoryManager {

	private Logger logger = LogManager.getRootLogger();
	
	private Path repositoryRoot;

	private RepositoryScaner repositoryScaner;
	
	private RepositoryVisitor repositoryVisitor;
	
	private BaseRepositoryOperations bro;

	public MasterRepositoryManager(RepositoryVisitor repositoryVisitor, BaseRepositoryOperations bro, AppProperties appProperties) {
		repositoryRoot = appProperties.getRepositoryRoot();
		
		this.bro = bro;
		this.repositoryVisitor = repositoryVisitor;
	}
	
	/**
	 * Rescans the whole repository and recreates repository data.repo where the
	 * records corresponding to all files are.
	 */
	private List<String> scan() {
		repositoryVisitor.reset();
		try {
			Files.walkFileTree(repositoryRoot, repositoryVisitor);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return repositoryVisitor.getFilesList();
	}

	/**
	 * Initializes Recreates data.repo file. Existed file is replaced by an
	 * empty one.
	 */
	private void init() {
		Path configPath = repositoryRoot.resolve("data.repo");
		try (OutputStream os = Files.newOutputStream(configPath);) {

		} catch (IOException e) {
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
			for(;;) {
				if(status == RepositoryScannerStatus.BUSY) {
					logger.info("[" + this.getClass().getSimpleName() + "] scan started");
				
					init();
					writeAll(scan());
				
					status = RepositoryScannerStatus.READY;
					logger.info("[" + this.getClass().getSimpleName() + "] scan ended");
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
