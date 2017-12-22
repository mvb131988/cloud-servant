package transfer.constant;

public enum SlaveTransferThreadStatus {

	BUSY(1),
	READY(2);
	
	private int value;
	
	private SlaveTransferThreadStatus(int value) {
		this.value = value;
	}
}
