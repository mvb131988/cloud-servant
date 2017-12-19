package repository.status;

/**
 * When slave creates data.repo file it sets status RECEIVE_START, when file transfer terminates it
 * changes status to RECEIVE_END 
 */
public enum RepoFileStatus {

	RECEIVE_START(1),
	RECEIVE_END(2);
	
	private int value;

	private RepoFileStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static RepoFileStatus to(int i) {
		for (RepoFileStatus repoFileStatus : RepoFileStatus.values()) {
			if (repoFileStatus.getValue() == i) {
				return repoFileStatus;
			}
		}
		return null;
	}
	
}
