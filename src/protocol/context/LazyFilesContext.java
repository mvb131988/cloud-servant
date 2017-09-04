package protocol.context;

import java.util.List;

import file.repository.metadata.RepositoryRecord;
import transformer.FilesContextTransformer;

/**
 * Returns first absent file (in slave repository) at the time it is found.
 * Compared to FilesContext doesn't create the whole list at a time. 
 */
public class LazyFilesContext implements FilesContext {

	private List<RepositoryRecord> records;
	private int currentPosition = 0;
	
	private FilesContextTransformer fct;
	
	public LazyFilesContext(List<RepositoryRecord> records, FilesContextTransformer fct) {
		this.records = records;
		this.fct = fct;
	}
	
	public boolean hasNext() {
		return currentPosition < records.size();
	}
	
	public FileContext next() {
		return fct.transform(records.get(currentPosition++));
	}
	
}
