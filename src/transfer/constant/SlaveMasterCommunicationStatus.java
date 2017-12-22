package transfer.constant;

public enum SlaveMasterCommunicationStatus {

	BUSY(1),
	READY(2);
	
	private int value;
	
	private SlaveMasterCommunicationStatus(int value) {
		this.value = value;
	}
	
}
