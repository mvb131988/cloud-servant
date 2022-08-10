package repository;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.BaseRepositoryOperations.AsynchronySearcher;
import repository.BaseRepositoryOperations.RepositoryConsistencyChecker;
import repository.status.RepositoryFileStatus;
import repository.status.RepositoryStatusMapper;
import repository.status.SlaveRepositoryManagerStatus;

public class SlaveRepositoryManager {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;
	
	private RepositoryStatusMapper sm;
	
	private AsynchronySearcher asynchronySearcher;
	
	private Thread repositoryScanerThread;
	
	public SlaveRepositoryManager(BaseRepositoryOperations bro, 
								  RepositoryStatusMapper sm) {
		super();
		this.bro = bro;
		this.sm = sm;
		repositoryScanerThread = new Thread(asynchronySearcher);
		repositoryScanerThread.setName("AsynchronySearcher");
	}
	
	/**
	 * ========================================================================================
	 */
	public SlaveRepositoryManagerStatus getStatus() {
		return sm.map(asynchronySearcher.getStatus());
	}
	
	/**
	 * Invoked before starting the execution
	 */
	public void reset(String memberId) {
		asynchronySearcher = bro.getAsynchronySearcher(memberId);
		repositoryScanerThread = new Thread(asynchronySearcher);
		repositoryScanerThread.setName("AsynchronySearcher");
		repositoryScanerThread.start();
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
	 * ========================================================================================
	 */
	
	//TODO: add check scan at the end of batch file transfer
	/**
	 * Scans slave repository and looks for divergency between record in data.repo file and actual
	 * file stored in file system. 
	 * 
	 * @throws IOException
	 */
	public void checkScan(String memberId) throws IOException {
		logger.info("[" + this.getClass().getSimpleName() + "] slave repo check started");
		RepositoryConsistencyChecker checker = bro.repositoryConsistencyChecker();
		RepositoryStatusDescriptor repoDescriptor = checker.check(memberId);
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] slave repo check started terminated");
		
		RepositoryFileStatus status = repoDescriptor.getRepositoryFileStatus();
		if(status == RepositoryFileStatus.RECEIVE_END) {
			logger.info("[" + this.getClass().getSimpleName() 
					  + "] slave repo report generation started");
			bro.writeRepositoryStatusDescriptor(repoDescriptor);
			logger.info("[" + this.getClass().getSimpleName() 
					  + "] slave repo report generation terminated");
			
			//TODO: step 3
			//remove corrupted files
		}
	}
	
}
