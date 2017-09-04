package transformer;

import java.nio.file.Path;
import java.nio.file.Paths;

import file.repository.metadata.RepositoryRecord;
import protocol.context.FileContext;

public class FilesContextTransformer {

	private Path repositoryRoot = Paths.get("C:\\temp");
	
	public FileContext transform(RepositoryRecord rr) {
		return (new FileContext.Builder())
				.setRepositoryRoot(repositoryRoot)
				.setRelativePath(Paths.get(rr.getFileName()))
				.build(); 
	}
	
}
