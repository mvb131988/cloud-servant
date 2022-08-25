package repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.FilePathMaxLengthException;
import main.AppProperties;
import transfer.TransferManagerStateMonitor;

/**
 * Main class that scans the repository and creates memberId_data.repo
 * 
 * memberId_data.repo file - file with the list of all files saved in the repository
 * 							 of the given member.
 */
public class RepositoryManager {

	private final int smallTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private Path repositoryRoot;

	private RepositoryScaner repositoryScaner;
	
	private RepositoryVisitor repositoryVisitor;
	
	private TransferManagerStateMonitor tmsm;
	
	private BaseRepositoryOperations bro;
	
	private String memberId;

	public RepositoryManager(RepositoryVisitor repositoryVisitor, 
							 BaseRepositoryOperations bro,
							 TransferManagerStateMonitor tmsm,
							 AppProperties appProperties) 
	{
		repositoryRoot = appProperties.getRepositoryRoot();
		
		this.bro = bro;
		this.tmsm = tmsm;
		this.repositoryVisitor = repositoryVisitor;
		this.smallTimeout = appProperties.getSmallPoolingTimeout();
		
		this.memberId = appProperties.getMemberId();
	}
	
	/**
	 * Rescans the whole repository and recreates repository data.repo where the
	 * records corresponding to all files are.
	 * @throws IOException 
	 */
	private List<RepositoryRecord> scan() throws IOException {
		repositoryVisitor.reset();
		Files.walkFileTree(repositoryRoot, repositoryVisitor);
		return repositoryVisitor.getFilesList();
	}

	/**
	 * Writes the list of files into data.repo
	 * 
	 * @param fileNames
	 *            - list of files(relative names) to be written into data.repo
	 * @throws FilePathMaxLengthException 
	 * @throws IOException 
	 */
	private void writeAll(List<RepositoryRecord> fileNames) 
			throws IOException, FilePathMaxLengthException 
	{
		bro.writeAll(fileNames, 0);
	}

	public RepositoryScaner getScaner() {
		if(repositoryScaner == null) {
			return new RepositoryScaner(tmsm);
		}
		return repositoryScaner;
	}
	
	//TODO(FUTURE): Don't get all file names into the memory at once.
	//Instead implement (partial scan->flush) process
	public class RepositoryScaner implements Runnable {

		private TransferManagerStateMonitor tmsm;
		
		private RepositoryScaner(TransferManagerStateMonitor tmsm) {
			this.tmsm = tmsm;
		}
		
		@Override
		public void run() {
			try {
				for(;;) {
					try {
						if(tmsm.lock()) {
							
							logger.info("[" + this.getClass().getSimpleName() + "] scan started");
						
							writeAll(scan());
						
							logger.info("[" + this.getClass().getSimpleName() + "] scan ended");
						}
					} finally {
						tmsm.unlock();
					}
					
					//TODO: Check and remove
					//RepositoryRecord rr = bro.read(BaseRepositoryOperations.HEADER_SIZE + 
					//								 13*RecordConstants.FULL_SIZE);
					//rr.getId();
					
					//Thread idle timeout.
					//Wait 1 second to avoid resources overconsumption.
					Thread.sleep(smallTimeout);
				}
			}
			catch (Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}
		
	}
	
}
