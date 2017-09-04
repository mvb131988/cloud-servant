package protocol.context;

import java.util.List;

import file.repository.metadata.RepositoryRecord;

/**
 * Returns first absent file (in slave repository) at the time it is found.
 * Compared to FilesContext doesn't create the whole list at a time. 
 */
public class LazyFilesContext {

	private List<RepositoryRecord> records;
	
	public LazyFilesContext(List<RepositoryRecord> records) {
		this.records = records;
	}
	
	public boolean hasNext() {
		return true;
	}
	
	public FileContext next() {
		return null;
	}
	
}
