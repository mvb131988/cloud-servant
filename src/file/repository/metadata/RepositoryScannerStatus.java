package file.repository.metadata;

public enum RepositoryScannerStatus {

	BUSY(1),
	READY(2);
		
	private int value;
	
	private RepositoryScannerStatus(int value) {
		this.value = value;
	}
	
}
