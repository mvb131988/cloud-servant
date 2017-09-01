package protocol.context;

import java.nio.file.Path;

@Deprecated
public class FileTransferOperationContext {

	private Path repositoryRoot;
	private FilesContext filesContext;

	private FileTransferOperationContext(Path repositoryRoot, FilesContext filesContext) {
		super();
		this.repositoryRoot = repositoryRoot;
		this.filesContext = filesContext;
	}

	public Path getRepositoryRoot() {
		return repositoryRoot;
	}
	
	public FilesContext getFilesContext() {
		return filesContext;
	}

	public static class Builder {

		private Path repositoryRoot;
		private FilesContext filesContext;

		public Builder setRepositoryRoot(Path repositoryRoot) {
			this.repositoryRoot = repositoryRoot;
			return this;
		}

		public Builder setFilesContext(FilesContext filesContext) {
			this.filesContext = filesContext;
			return this;
		}

		public FileTransferOperationContext build() {
			return new FileTransferOperationContext(repositoryRoot, filesContext);
		}

	}

}
