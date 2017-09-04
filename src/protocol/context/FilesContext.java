package protocol.context;

import java.util.ArrayList;
import java.util.List;

// Extract interface from
public class FilesContext {

	private List<FileContext> fcList = new ArrayList<>();
	private int currentPos = -1;
	private int currentLength = 0;
	
	public boolean hasNext() {
		return (currentPos > -1) && (currentPos < currentLength);
	}
	
	public FileContext next() {
		return fcList.get(++currentPos);
	}
	
	public void add(FileContext fc) {
		fcList.add(fc);
		currentLength++;
	}
}
