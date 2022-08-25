package repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import main.AppProperties;
import repository.status.RepositoryStatusMapper;
import repository.status.SlaveRepositoryManagerStatus;
import transformer.FilesContextTransformer;

public class AsynchronySearcherManager {

	private Logger logger = LogManager.getRootLogger();
	
	private AsynchronySearcher as;
	
	private RepositoryStatusMapper sm;
	
	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	private final int smallTimeout;
	
	public AsynchronySearcherManager(BaseRepositoryOperations bro,
			  						 FilesContextTransformer fct,
			  						 RepositoryStatusMapper sm,
			  						 AppProperties ap) {
		this.bro = bro;
		this.fct = fct;
		this.sm = sm;
		this.smallTimeout = ap.getSmallPoolingTimeout();
	}
	
	/**
	 * Creates thread that identifies difference between member's local repository and neighbour 
	 * member repository, that is identified by it's data.repo file (memberId identifies what 
	 * data.repo file is subject to scan).
	 * 
	 * @param memberId
	 * @return
	 */
	public void startRepoAsyncSearcherThread(String memberId) {
		as = new AsynchronySearcher(bro, fct, memberId, smallTimeout);
		Thread repositoryScanerThread = new Thread(as);
		repositoryScanerThread.setName("AsynchronySearcher");
		repositoryScanerThread.start();
	}
	
	/**
	 * Returns next repository record that doesn't have corresponding file in slave repository.
	 * Non-blocking
	 */
	public RepositoryRecord next() {
		RepositoryRecord rr = null;
		
		if(repoAsyncSearcherThreadStatus() == SlaveRepositoryManagerStatus.BUSY) {
			rr = as.nextAsynchrony();
		}
		
		return rr;
	}
	
	public SlaveRepositoryManagerStatus repoAsyncSearcherThreadStatus() {
		return sm.map(as.getStatus());
	}	
	
}
