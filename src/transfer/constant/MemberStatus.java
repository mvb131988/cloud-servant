package transfer.constant;

public enum MemberStatus {

	BUSY(1), 
	READY(2);

	private int value;

	private MemberStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static MemberStatus to(int i) {
		for (MemberStatus ms : MemberStatus.values()) {
			if (ms.value == i) {
				return ms;
			}
		}
		return null;
	}
}
