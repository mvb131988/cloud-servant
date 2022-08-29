package repository;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.status.AsynchronySearcherStatus;
import transformer.FilesContextTransformer;

/**
 * asynchrony - is a file that exists in memberId_data.repo and doesn't in member local repository.
 */
// TODO(FUTURE): Delete operation isn't implemented
public class AsynchronySearcher implements Runnable {

	private final static int BATCH_SIZE = 10000;
	
	private Logger logger = LogManager.getRootLogger();
	
	// Records that don't have corresponding file in the repository
	private Queue<RepositoryRecord> asynchronyBuffer;

	private BaseRepositoryOperations bro;
	
	private FilesContextTransformer fct;
	
	// max asynchrony buffer size
	private final int asynchronyBufferMaxSize = 500;

	private AsynchronySearcherStatus asynchronySearcherStatus;

	private String memberId;
	
	private final int smallTimeout;
	
	public AsynchronySearcher(BaseRepositoryOperations bro,
							  FilesContextTransformer fct,
							  String memberId,
							  int smallTimeout) 
	{
		asynchronyBuffer = new ArrayBlockingQueue<>(asynchronyBufferMaxSize);
		asynchronySearcherStatus = AsynchronySearcherStatus.READY;
		
		this.bro = bro;
		this.fct = fct;
		
		this.smallTimeout = smallTimeout;
		this.memberId = memberId;
	}

	@Override
	public void run() {
		try{
			asynchronySearcherStatus = AsynchronySearcherStatus.BUSY;

			byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];

			InputStream is = bro.openDataRepo(memberId);
			bro.readDataRepoHeader(is);
			int readBytes = 0;
			while ((readBytes = bro.next(is, buffer)) != -1) {
				List<RepositoryRecord> records = fct.transform(buffer, readBytes);
				for (RepositoryRecord rr : records) {

					// Set current record path to be used for file transfer
					if (!bro.existsFile(Paths.get(rr.getFileName()))) {
						
						// if previous read asynchrony isn't consumed wait until
						// it is consumed
						while (bufferFull()) {
							// Waiting until messages are consumed from a full buffer.
							Thread.sleep(smallTimeout);
						}
						setAsynchrony(rr);
					}

					// If file for record exists skip it move to next one
					if (bro.existsFile(Paths.get(rr.getFileName()))) {
						// log as existed one
					}
				}
			}
			bro.closeDataRepo(is);

			// Last read. Wait until last read record isn't consumed.
			while (!bufferEmpty()) {
				
				// Waiting until all messages are consumed from a buffer.
				Thread.sleep(smallTimeout);
			}

			asynchronySearcherStatus = AsynchronySearcherStatus.TERMINATED;
		} 
		catch(Exception e) {
			logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
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