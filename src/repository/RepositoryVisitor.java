package repository;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import main.AppProperties;

public class RepositoryVisitor implements FileVisitor<Path> {

	private List<String> filesList = new ArrayList<String>();
	
	private Path repositoryRoot;

	public RepositoryVisitor(AppProperties appProperties) {
		repositoryRoot = appProperties.getRepositoryRoot();
	}
	
	public void reset() {
		filesList = new ArrayList<String>();
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(dir.getFileName().toString().equals(".log")) {
			//filter out all system folders
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(file.getFileName().toString().equals("data.repo")) {
			//filter out data.repo
		}
		else {
			filesList.add(repositoryRoot.relativize(file).toString());
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

	public List<String> getFilesList() {
		return filesList;
	}

	public void setFilesList(List<String> filesList) {
		this.filesList = filesList;
	}

}
