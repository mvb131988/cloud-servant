package transfer.context;

import java.nio.file.Path;

public class FileContext {
	
	private Path relativePath;
	private long size;
	private long creationDateTime;
	
	public FileContext(Path relativePath, long size, long creationDateTime) {
		super();
		this.relativePath = relativePath;
		this.size = size;
		this.creationDateTime = creationDateTime;
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

		private Path relativeName;
		private long size;
		private long creationDateTime;

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
			return new FileContext(relativeName, size, creationDateTime);
		}

	}
	
}
