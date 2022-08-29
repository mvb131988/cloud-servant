package repository.status;

/**
 * When SOURCE or CLOUD member scans its own repository it creates memberId_data.repo file and 
 * sets its status RECEIVE_START, when memberId_data.repo file is transfered to external member,
 * external member sets its status to RECEIVE_END.
 */
public enum RepositoryFileStatus {

	RECEIVE_START(1),
	RECEIVE_END(2);
	
	private int value;

	private RepositoryFileStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static RepositoryFileStatus to(int i) {
		for (RepositoryFileStatus repoFileStatus : RepositoryFileStatus.values()) {
			if (repoFileStatus.getValue() == i) {
				return repoFileStatus;
			}
		}
		return null;
	}
	
}
