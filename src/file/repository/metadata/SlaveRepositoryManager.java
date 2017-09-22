package file.repository.metadata;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import file.repository.metadata.BaseRepositoryOperations.AsynchronySearcher;
import file.repository.metadata.status.SlaveRepositoryManagerStatus;
import file.repository.metadata.status.RepositoryStatusMapper;

public class SlaveRepositoryManager {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;
	
	private RepositoryStatusMapper sm;
	
	private AsynchronySearcher asynchronySearcher;
	
	private Thread repositoryScanerThread;
	
	public SlaveRepositoryManager(BaseRepositoryOperations bro, RepositoryStatusMapper sm) {
		super();
		this.bro = bro;
		this.sm = sm;
		asynchronySearcher = bro.getAsynchronySearcher();
		repositoryScanerThread = new Thread(asynchronySearcher);
	}

	public void init() {
		// Create temporary folder where incoming files firstly will be saved
		Path temp = Paths.get(".temp");
		bro.createDirectoryIfNotExist(temp);
		bro.hideDirectory(temp);
	}
	
	/**
	 * ==========================================================================================
	 */
	public SlaveRepositoryManagerStatus getStatus() {
		return sm.map(asynchronySearcher.getStatus());
	}
	
	/**
	 * Invoked before starting the execution
	 */
	public void reset() {
		if (getStatus() == SlaveRepositoryManagerStatus.READY) {
			repositoryScanerThread.start();
		}
		if(getStatus() == SlaveRepositoryManagerStatus.TERMINATED) {
			asynchronySearcher = bro.getAsynchronySearcher();
			repositoryScanerThread = new Thread(asynchronySearcher);
			repositoryScanerThread.start();
		}
	}
	
	/**
	 * Returns next repository record that doesn't have corresponding file in slave repository. 
	 * Non-blocking
	 */
	public RepositoryRecord next() {
		RepositoryRecord rr = null;
		
		if(getStatus() == SlaveRepositoryManagerStatus.BUSY) {
			rr = asynchronySearcher.nextAsynchrony();
		}
		
		return rr;
	}
	/**
	 * ==========================================================================================
	 */
	
}
