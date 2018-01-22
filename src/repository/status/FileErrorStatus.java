package repository.status;

public enum FileErrorStatus {

	NOT_EXIST(1),
	SIZE_MISMATCH(2),
	CREATION_DATE_MISMATH(3);
	
	private int errorCode;
	
	private FileErrorStatus(int errorCode) {
		this.errorCode = errorCode;
	}
	
}
