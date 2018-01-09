package repository;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.BaseRepositoryOperations.AsynchronySearcher;
import repository.status.RepositoryStatusMapper;
import repository.status.SlaveRepositoryManagerStatus;

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
		repositoryScanerThread.setName("AsynchronySearcher");
	}

	public void init() {
		try {
			//TODO: Move to initializer
			
			// Create log directory
			Path log = Paths.get(".log");
			bro.createDirectoryIfNotExist(log);
			bro.hideDirectory(log);
			
			// Create temporary folder where incoming files firstly will be saved
			Path temp = Paths.get(".temp");
			bro.createDirectoryIfNotExist(temp);
			bro.hideDirectory(temp);
		} 
		catch (Exception e) {
			logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e);
		}
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
			repositoryScanerThread.setName("AsynchronySearcher");
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
	
	//repository check scan(consistency scan) goes here 
	public void checkScan() {
		//step1
		//Delegate to another class (RepositoryConsistencyChecker) create in BaseRepositoryOperations
		//check data repo file status
		//for each record from data.repo(DataRepo iterator) compare records parameter with actual file parameters
		//create FileDescriptor for this
		//create and return RepositoryDescriptor to reflect repository state
		
		//step2
		//save RepositoryDescriptor into a file in /.sys
		
		//step3
		//remove corrupted files
	}
	
}
