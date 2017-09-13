package file.repository.metadata.status;

public enum SlaveRepositoryManagerStatus {

	BUSY(1), 
	READY(2),
	TERMINATED(3);

	private int value;

	private SlaveRepositoryManagerStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static SlaveRepositoryManagerStatus to(int i) {
		for (SlaveRepositoryManagerStatus srms : SlaveRepositoryManagerStatus.values()) {
			if (srms.getValue() == i) {
				return srms;
			}
		}
		return null;
	}
}
