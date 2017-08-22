package file.repository.metadata;

import java.util.Set;

public class RecordSet {
	
	private RepositoryManager repositoryManager;
	
	private Set<String> names;
	
	public RecordSet(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
		names = repositoryManager.readNames();
	}
	
	/**
	 * 
	 * @param name
	 * @return true if record exists and remove it.
	 * 		   false if record doesn't exist.
	 */
	public boolean removeIfExists(String name) {
		return true;
	}
	
}
