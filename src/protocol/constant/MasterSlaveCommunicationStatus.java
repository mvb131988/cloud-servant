package protocol.constant;

public enum MasterSlaveCommunicationStatus {

	BUSY(1),
	READY(2);
	
	private int value;
	
	private MasterSlaveCommunicationStatus(int value) {
		this.value = value;
	}
}
