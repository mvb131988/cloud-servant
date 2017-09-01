package protocol.context;

import java.util.ArrayList;
import java.util.List;

// Extract interface from
public class FilesContext {

	private List<FileContext> fcList = new ArrayList<>();
	
	public boolean hasNext() {
		return true;
	}
	
	public FileContext next() {
		return fcList.get(0);
	}
	
	public void add(FileContext fc) {
		fcList.add(fc);
	}
}
