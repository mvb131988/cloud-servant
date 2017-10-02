package transfer.constant;

public enum MasterStatus {

	BUSY(1), 
	READY(2);

	private int value;

	private MasterStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static MasterStatus to(int i) {
		for (MasterStatus ms : MasterStatus.values()) {
			if (ms.value == i) {
				return ms;
			}
		}
		return null;
	}
}
