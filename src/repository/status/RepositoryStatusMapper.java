package repository.status;

public class RepositoryStatusMapper {

	public SlaveRepositoryManagerStatus map(AsynchronySearcherStatus in) {
		SlaveRepositoryManagerStatus out = null;

		switch (in) {
		case BUSY:
			out = SlaveRepositoryManagerStatus.BUSY;
			break;
		case READY:
			out = SlaveRepositoryManagerStatus.READY;
			break;
		case TERMINATED: 
			out = SlaveRepositoryManagerStatus.TERMINATED;
			break;
		default:
			break;
		}

		return out;
	}
	
}
