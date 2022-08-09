package repository;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import main.AppProperties;

public class RepositoryVisitor implements FileVisitor<Path> {

	private List<RepositoryRecord> filesList = new ArrayList<RepositoryRecord>();
	
	private Path repositoryRoot;
	
	private BaseRepositoryOperations bro;

	public RepositoryVisitor(BaseRepositoryOperations bro, AppProperties appProperties) {
		this.bro = bro;
		repositoryRoot = appProperties.getRepositoryRoot();
	}
	
	public void reset() {
		filesList = new ArrayList<RepositoryRecord>();
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(dir.getFileName().toString().equals(".log") ||
		   dir.getFileName().toString().equals(".sys") ||
		   dir.getFileName().toString().equals(".temp")) 
		{
			//filter out all system folders
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(file.getFileName().toString().contains("data.repo")) {
			//filter out data.repo
		}
		else {
			String path = repositoryRoot.relativize(file).toString();
			
			//change to Linux compatible slashes
			path = path.replaceAll("\\\\", "\\/");
			
			RepositoryRecord rr = new RepositoryRecord();
			rr.setFileName(path);
			Path pPath = Paths.get(path);
			rr.setSize(bro.getSize(Paths.get(path)));
			rr.setMillisCreationDate(bro.getCreationDateTime(pPath));
			
			filesList.add(rr);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public List<RepositoryRecord> getFilesList() {
		return filesList;
	}

	public void setFilesList(List<RepositoryRecord> filesList) {
		this.filesList = filesList;
	}

}
