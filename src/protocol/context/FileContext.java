package protocol.context;

import java.nio.file.Path;

public class FileContext {
	
	private Path repositoryRoot;
	private Path relativePath;
	private long size;
	private long creationDateTime;
	
	public FileContext(Path repositoryRoot, Path relativePath, long size, long creationDateTime) {
		super();
		this.repositoryRoot = repositoryRoot;
		this.relativePath = relativePath;
		this.size = size;
		this.creationDateTime = creationDateTime;
	}

	public Path getRepositoryRoot() {
		return repositoryRoot;
	}
	
	public Path getRelativePath() {
		return relativePath;
	}

	public long getSize() {
		return size;
	}

	public long getCreationDateTime() {
		return creationDateTime;
	}
	
	public static class Builder {

		private Path repositoryRoot;
		private Path relativeName;
		private long size;
		private long creationDateTime;

		public Builder setRepositoryRoot(Path repositoryRoot) {
			this.repositoryRoot = repositoryRoot;
			return this;
		}
		
		public Builder setRelativePath(Path relativeName) {
			this.relativeName = relativeName;
			return this;
		}

		public Builder setSize(long size) {
			this.size = size;
			return this;
		}

		public Builder setCreationDateTime(long creationDateTime) {
			this.creationDateTime = creationDateTime;
			return this;
		}

		public FileContext build() {
			return new FileContext(repositoryRoot, relativeName, size, creationDateTime);
		}

	}
	
}
