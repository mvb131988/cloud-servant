package file.repository;

public class RepositoryRecord {

	private long id;
	private long fileameSize;
	private String fileName;
	private byte status;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getFileameSize() {
		return fileameSize;
	}

	public void setFileameSize(long fileameSize) {
		this.fileameSize = fileameSize;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

}
