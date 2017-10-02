package repository.status;

public enum AsynchronySearcherStatus {

	BUSY(1), 
	READY(2),
	TERMINATED(3);

	private int value;

	private AsynchronySearcherStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static AsynchronySearcherStatus to(int i) {
		for (AsynchronySearcherStatus asynchronySearcherStatus : AsynchronySearcherStatus.values()) {
			if (asynchronySearcherStatus.getValue() == i) {
				return asynchronySearcherStatus;
			}
		}
		return null;
	}
	
}
