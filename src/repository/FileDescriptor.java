package repository;

import repository.status.FileErrorStatus;

public class FileDescriptor {

	private RepositoryRecord repositoryRecord;
	
	private FileErrorStatus fileErrorStatus;

	public FileDescriptor() {
		
	}
	
	public FileDescriptor(RepositoryRecord repositoryRecord, FileErrorStatus fileErrorStatus) {
		super();
		this.repositoryRecord = repositoryRecord;
		this.fileErrorStatus = fileErrorStatus;
	}

	public RepositoryRecord getRepositoryRecord() {
		return repositoryRecord;
	}

	public void setRepositoryRecord(RepositoryRecord repositoryRecord) {
		this.repositoryRecord = repositoryRecord;
	}

	public FileErrorStatus getFileErrorStatus() {
		return fileErrorStatus;
	}

	public void setFileErrorStatus(FileErrorStatus fileErrorStatus) {
		this.fileErrorStatus = fileErrorStatus;
	}
	
}
