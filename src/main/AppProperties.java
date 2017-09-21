package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class AppProperties {

	private boolean isMaster;
	
	private Path repositoryRoot;
	
	public AppProperties() {
		repositoryRoot = Paths.get(ResourceBundle.getBundle("app").getString("root"));
		isMaster = Boolean.parseBoolean(ResourceBundle.getBundle("app").getString("master"));	
	}

	public boolean isMaster() {
		return isMaster;
	}

	public Path getRepositoryRoot() {
		return repositoryRoot;
	}
	
}
