package file.repository.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class FilePropertyLookupService {

	private Path repositoryRoot = Paths.get("D:\\temp");
	
	public long getSize(Path relativePath) {
		long size = 0;
		try {
			size = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).size();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}
	
	public long getCreationDateTime(Path relativePath) {
		long creationDateTime = 0;
		try {
			creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return creationDateTime;
	}
	
	// TODO: Move it out
	public Path getRepositoryRoot() {
		return repositoryRoot;
	}
	
	
}
