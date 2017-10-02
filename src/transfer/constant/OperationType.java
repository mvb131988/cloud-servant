package transfer.constant;

public enum OperationType {

	REQUEST_FILE_START(1), 
	REQUEST_FILE_END(2), 
	RESPONSE_FILE_START(3), 
	RESPONSE_FILE_END(4),
	REQUEST_BATCH_START(5), 
	REQUEST_BATCH_END(6),
	RESPONSE_BATCH_START(7), 
	RESPONSE_BATCH_END(8),
	REQUEST_TRANSFER_START(9),
	REQUEST_TRANSFER_END(10),
	RESPONSE_TRANSFER_START(9),
	RESPONSE_TRANSFER_END(10),
	REQUEST_MASTER_STATUS_START(11),
	REQUEST_MASTER_STATUS_END(12),
	RESPONSE_MASTER_STATUS_START(13),
	RESPONSE_MASTER_STATUS_END(14);

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
