package repository;

import java.time.ZonedDateTime;
import java.util.List;

import repository.status.RepositoryFileStatus;

public class RepositoryStatusDescriptor {

	private RepositoryFileStatus repositoryFileStatus;
	
	private ZonedDateTime checkDateTime;
	
	private ZonedDateTime dataRepoDateTime;
	
	private int numberOfFiles;
	
	private long totalSize;
	
	private int numberOfCorruptedFiles;
	
	private List<FileDescriptor> corruptedFiles;

	public ZonedDateTime getCheckDateTime() {
		return checkDateTime;
	}

	public void setCheckDateTime(ZonedDateTime checkDateTime) {
		this.checkDateTime = checkDateTime;
	}

	public ZonedDateTime getDataRepoDateTime() {
		return dataRepoDateTime;
	}

	public void setDataRepoDateTime(ZonedDateTime dataRepoDateTime) {
		this.dataRepoDateTime = dataRepoDateTime;
	}

	public int getNumberOfFiles() {
		return numberOfFiles;
	}

	public void setNumberOfFiles(int numberOfFiles) {
		this.numberOfFiles = numberOfFiles;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public int getNumberOfCorruptedFiles() {
		return numberOfCorruptedFiles;
	}

	public void setNumberOfCorruptedFiles(int numberOfCorruptedFiles) {
		this.numberOfCorruptedFiles = numberOfCorruptedFiles;
	}

	public List<FileDescriptor> getCorruptedFiles() {
		return corruptedFiles;
	}

	public void setCorruptedFiles(List<FileDescriptor> corruptedFiles) {
		this.corruptedFiles = corruptedFiles;
	}

	public RepositoryFileStatus getRepositoryFileStatus() {
		return repositoryFileStatus;
	}

	public void setRepositoryFileStatus(RepositoryFileStatus repositoryFileStatus) {
		this.repositoryFileStatus = repositoryFileStatus;
	}
	
}
