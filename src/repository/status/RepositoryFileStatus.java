package repository.status;

/**
 * When slave creates data.repo file it sets status RECEIVE_START, when file transfer terminates it
 * changes status to RECEIVE_END 
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
