package repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.status.FileErrorStatus;
import repository.status.RepositoryFileStatus;
import transformer.FilesContextTransformer;
import transformer.LongTransformer;

/**
 * For each record from neighbour member (identified by its data.repo) looks up
 * for a corresponding file in the local repository and checks whether all
 * properties (size, creation date) from the record are identical to all
 * properties of the file. Returns RepositoryDescriptor with list of files that
 * don't math this rule plus some additional info about current repository
 * state(like scan date, current repository date, number of files in the
 * repository, their size and so on...)
 **/
public class RepositoryConsistencyChecker {
	
	private final static int BATCH_SIZE = 10000;
	
	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;
	
	private LongTransformer lt;
	
	private FilesContextTransformer fct;
	
	public RepositoryConsistencyChecker(BaseRepositoryOperations bro, 
										LongTransformer lt, 
										FilesContextTransformer fct) 
	{
		this.bro = bro;
		this.lt = lt;
		this.fct = fct;
	}
	
	private RepositoryStatusDescriptor check(String memberId) {
		int recordsCounter = 0;
		long totalSize = 0;
		//TODO(FUTURE): In scan is done on empty repo and data.repo list is huge can lead
		//to memory over consumption. Needs to be implemented lazily.
		List<FileDescriptor> corruptedFiles = new ArrayList<>();
		RepositoryStatusDescriptor repoDescriptor = new RepositoryStatusDescriptor();
		DataRepoIterator iterator = null;
		
		try {
			iterator = new DataRepoIterator(memberId);
			RepositoryFileStatus repoFileStatus = 
					getHeaderCreationStatus(iterator.getHeader());
			repoDescriptor.setRepositoryFileStatus(repoFileStatus);
			
			if (repoFileStatus == RepositoryFileStatus.RECEIVE_END) {
				while (iterator.hasNextRecord()) {
					RepositoryRecord rr = iterator.nextRecord();

					// check record
					FileDescriptor fd = check(rr);
					if (fd != null) {
						corruptedFiles.add(fd);
					} else {
						recordsCounter++;
						totalSize += bro.getSize(Paths.get(rr.getFileName()));
					}
				}

				repoDescriptor.setCheckDateTime(ZonedDateTime.now());
				repoDescriptor.setDataRepoDateTime(
						getHeaderCreationTimestamp(iterator.getHeader()));
				repoDescriptor.setNumberOfFiles(recordsCounter);
				repoDescriptor.setTotalSize(totalSize);
				repoDescriptor.setNumberOfCorruptedFiles(corruptedFiles.size());
				repoDescriptor.setCorruptedFiles(corruptedFiles);
			}
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName()+ "] i/o exception during local"
					+ " member repository scan", e);
		}
		finally {
			try {
				if(iterator != null) {
					iterator.close();
				}
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName()+ "] i/o exception during "
						+ "local member repository scan", e);
			}
		}
		
		return repoDescriptor;
	}
	
	/**
	 * 
	 * @return null - if file is valid
	 * 		   FileDescriptor - if file is invalid
	 * @throws IOException 
	 */
	private FileDescriptor check(RepositoryRecord rr) throws IOException {
		FileDescriptor fd = null;
		Path p = Paths.get(rr.getFileName());
		long size = -1;
		long creationDateTime = -1;
		
		if(!bro.existsFile(p)) {
			fd = new FileDescriptor(rr, FileErrorStatus.NOT_EXIST);
		} 
		else if((size = bro.getSize(p)) != rr.getSize()) {
			fd = new FileDescriptor(rr, FileErrorStatus.SIZE_MISMATCH);
			fd.setActualSize(size);
		}
		else if((creationDateTime = bro.getCreationDateTime(p)) != rr.getMillisCreationDate()) {
			fd = new FileDescriptor(rr, FileErrorStatus.CREATION_DATE_MISMATH);
			fd.setMillisActualCretionDateTime(creationDateTime);
		}
		return fd;
	}
	
	/**
	 * Extracts data.repo file status from data.repo header 
	 * 
	 * @return data.repo file status
	 */
	private RepositoryFileStatus getHeaderCreationStatus(byte[] header) {
		RepositoryFileStatus status = RepositoryFileStatus.to(header[RecordConstants.TIMESTAMP]);
		return status;
	}
	
	/**
	 * Extracts data.repo file creation date time from data.repo header 
	 * 
	 * @return data.repo file creation date time
	 */
	private ZonedDateTime getHeaderCreationTimestamp(byte[] header) {
		byte[] bTimestamp = new byte[RecordConstants.TIMESTAMP];
		System.arraycopy(header, 0, bTimestamp, 0, RecordConstants.TIMESTAMP);
		long timestamp = lt.extractLong(bTimestamp);
		Instant i = Instant.ofEpochMilli(timestamp);
		return ZonedDateTime.ofInstant(i, ZoneOffset.systemDefault());
	}
	
	/**
	 *	Iterates throughout data.repo and returns next record if available until end of
	 *	data.repo is not reached
	 */
	private class DataRepoIterator {
		
		private List<RepositoryRecord> records;
		
		//position of the current record in the array records
		private int iRecord;
		
		private InputStream is;
		
		private byte[] header;
		
		private byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
		
		public DataRepoIterator(String memberId) throws IOException {
			is = bro.openDataRepo(memberId);
			header = bro.readDataRepoHeader(is);
		}
		
		public RepositoryRecord nextRecord() throws IOException {
			return records.get(iRecord++); 
		}
		
		public boolean hasNextRecord() throws IOException {
			if (records == null || iRecord == records.size()) {
				int readBytes = bro.next(is, buffer);

				if (readBytes == -1) {
					bro.closeDataRepo(is);
					return false;
				}

				records = fct.transform(buffer, readBytes);
				iRecord = 0;
			}

			return true;
		}
		
		public void close() throws IOException {
			bro.closeDataRepo(is);
		}

		public byte[] getHeader() {
			return header;
		}
		
	}
	
	/**
	 * Scans slave repository and looks for divergency between record in data.repo file and actual
	 * file stored in file system. 
	 * 
	 * @throws IOException
	 */
	public void checkScan(String memberId) throws IOException {
		logger.info("[" + this.getClass().getSimpleName() + "] slave repo check started");
		RepositoryStatusDescriptor repoDescriptor = check(memberId);
		logger.info("[" + this.getClass().getSimpleName() 
				  + "] slave repo check started terminated");
		
		RepositoryFileStatus status = repoDescriptor.getRepositoryFileStatus();
		if(status == RepositoryFileStatus.RECEIVE_END) {
			logger.info("[" + this.getClass().getSimpleName() 
					  + "] slave repo report generation started");
			bro.writeRepositoryStatusDescriptor(repoDescriptor, memberId);
			logger.info("[" + this.getClass().getSimpleName() 
					  + "] slave repo report generation terminated");
			
			//TODO: step 3
			//remove corrupted files
		}
	}
	
}
