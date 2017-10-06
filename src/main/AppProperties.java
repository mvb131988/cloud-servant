package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class AppProperties {

	private boolean isMaster;
	
	private Path repositoryRoot;
	
	private String masterIp;
	
	private Integer masterPort;
	
	public AppProperties() {
		repositoryRoot = Paths.get(ResourceBundle.getBundle("app").getString("root"));
		isMaster = Boolean.parseBoolean(ResourceBundle.getBundle("app").getString("master"));
		masterIp = ResourceBundle.getBundle("app").getString("master.ip");
		masterPort = Integer.parseInt(ResourceBundle.getBundle("app").getString("master.port"));
	}

	public boolean isMaster() {
		return isMaster;
	}

	public Path getRepositoryRoot() {
		return repositoryRoot;
	}

	public String getMasterIp() {
		return masterIp;
	}

	public Integer getMasterPort() {
		return masterPort;
	}
	
}
