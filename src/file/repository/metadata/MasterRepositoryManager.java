package file.repository.metadata;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ResourceBundle;

@Deprecated
public class MasterRepositoryManager implements FileVisitor<Path> {

	private RepositoryManager rm;

	private Path repositoryRoot = Paths.get(ResourceBundle.getBundle("app").getString("root"));

	private OutputStream os;
	
//	private final static int BATCH_SIZE = 10000;
//	
//	private byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];

	public MasterRepositoryManager() {
		super();
		this.rm = new RepositoryManager();
	}

	public void sync() {
		Path configPath = repositoryRoot.resolve("data.repo");

		try (OutputStream os = Files.newOutputStream(configPath)) {
			this.os = os;
			Files.walkFileTree(repositoryRoot, this);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		// TODO Auto-generated method stub
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		repositoryRoot.relativize(file).toString();
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		// TODO Auto-generated method stub
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		// TODO Auto-generated method stub
		return FileVisitResult.CONTINUE;
	}

}
