package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class AppProperties {

	private boolean isMaster;
	
	private Path repositoryRoot;
	
	//TODO: masterIp will be defined in several ways(first is scan of local network[will be default scenario]) 
	//TODO: leave this property like it is and create a separate flow for manual setting(for debugging purposes)
	private String masterIp;
	
	private Integer masterPort;
	
	//Admin port is common both for master and slave side.
	private Integer adminPort;
	
	private Integer masterRepositoryScheduleInterval;
	
	private Integer slaveTransferScheduleInterval;
	
	private Integer smallPoolingTimeout;
	
	private Integer bigPoolingTimeout;
	
	private Integer socketSoTimeout;
	
	private Integer localAutodetectionPeriod;
	
	public AppProperties() {
		repositoryRoot = Paths.get(ResourceBundle.getBundle("app").getString("root"));
		isMaster = Boolean.parseBoolean(ResourceBundle.getBundle("app").getString("master"));
		masterIp = ResourceBundle.getBundle("app").getString("master.ip");
		masterPort = Integer.parseInt(ResourceBundle.getBundle("app").getString("master.port"));
		adminPort = Integer.parseInt(ResourceBundle.getBundle("app").getString("admin.port"));
		masterRepositoryScheduleInterval = Integer.parseInt(ResourceBundle.getBundle("app").getString("schedule.master.repository"));
		slaveTransferScheduleInterval = Integer.parseInt(ResourceBundle.getBundle("app").getString("schedule.slave.transfer"));
		smallPoolingTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.pooling.small"));
		bigPoolingTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.pooling.big"));
		socketSoTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.so.socket"));
		localAutodetectionPeriod = Integer.parseInt(ResourceBundle.getBundle("app").getString("period.local"));
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

	public Integer getAdminPort() {
		return adminPort;
	}

	public Integer getMasterRepositoryScheduleInterval() {
		return masterRepositoryScheduleInterval;
	}

	public Integer getSlaveTransferScheduleInterval() {
		return slaveTransferScheduleInterval;
	}

	public Integer getSmallPoolingTimeout() {
		return smallPoolingTimeout;
	}

	public Integer getBigPoolingTimeout() {
		return bigPoolingTimeout;
	}

	public Integer getSocketSoTimeout() {
		return socketSoTimeout;
	}

	public void setMasterIp(String masterIp) {
		this.masterIp = masterIp;
	}

	public Integer getLocalAutodetectionPeriod() {
		return localAutodetectionPeriod;
	}
	
}
