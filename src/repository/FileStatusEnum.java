package repository;

public enum FileStatusEnum {

	LIVE((byte) 1), 
	REMOVED((byte) 2),
	//file was received by slave node, but not saved in repository.
	TRANSIENT((byte) 3);

	private final byte status;

	private FileStatusEnum(byte status) {
		this.status = status;
	}

}
