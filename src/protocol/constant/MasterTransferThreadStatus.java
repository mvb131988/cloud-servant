package protocol.constant;

public enum MasterTransferThreadStatus {

	BUSY(1),
	READY(2),
	TRANSIENT(3),
	EMPTY(4);
	
	private int value;
	
	private MasterTransferThreadStatus(int value) {
		this.value = value;
	}
	
}
