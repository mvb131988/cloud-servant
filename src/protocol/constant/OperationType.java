package protocol.constant;

public enum OperationType {

	REQUEST_FILE_START(1), 
	REQUEST_FILE_END(2), 
	RESPONSE_FILE_START(3), 
	RESPONSE_FILE_END(4);

	private int type;

	private OperationType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public static OperationType to(int i) {
		for (OperationType ot : OperationType.values()) {
			if (ot.type == i) {
				return ot;
			}
		}
		return null;
	}
}
