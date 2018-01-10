package repository.status;

public enum FileErrorStatus {

	NOT_EXIST(1);
	
	private int errorCode;
	
	private FileErrorStatus(int errorCode) {
		this.errorCode = errorCode;
	}
	
}
