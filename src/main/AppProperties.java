package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class AppProperties {
	
	private boolean isMaster;
	
	private Path repositoryRoot;
	
	private Integer masterPort;
	
	//Admin port is common both for master and slave side.
	private Integer adminPort;
	
	private Integer masterRepositoryScheduleInterval;
	
	private Integer slaveTransferScheduleInterval;
	
	private Integer smallPoolingTimeout;
	
	private Integer bigPoolingTimeout;
	
	private Integer socketSoTimeout;
	
	private Integer localAutodetectionPeriod;
	
	private Integer workPerThread;
	
	private Integer fjpSize;
	
	// Autodetection properties
	private String ISP_HOME = "homelocal";
	
	private String localRanges;
	
	public AppProperties() {
		repositoryRoot = Paths.get(ResourceBundle.getBundle("app").getString("root"));
		isMaster = Boolean.parseBoolean(ResourceBundle.getBundle("app").getString("master"));
		masterPort = Integer.parseInt(ResourceBundle.getBundle("app").getString("master.port"));
		adminPort = Integer.parseInt(ResourceBundle.getBundle("app").getString("admin.port"));
		masterRepositoryScheduleInterval = Integer.parseInt(ResourceBundle.getBundle("app").getString("schedule.master.repository"));
		slaveTransferScheduleInterval = Integer.parseInt(ResourceBundle.getBundle("app").getString("schedule.slave.transfer"));
		smallPoolingTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.pooling.small"));
		bigPoolingTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.pooling.big"));
		socketSoTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.so.socket"));
		localAutodetectionPeriod = Integer.parseInt(ResourceBundle.getBundle("app").getString("period.local"));
		
		workPerThread = Integer.parseInt(ResourceBundle.getBundle("app").getString("autodiscovery.unit.value"));
		fjpSize = Integer.parseInt(ResourceBundle.getBundle("app").getString("autodiscovery.pool.size"));
		
		localRanges = ResourceBundle.getBundle("ipranges").getString(ISP_HOME);
	}

	public boolean isMaster() {
		return isMaster;
	}

	public Path getRepositoryRoot() {
		return repositoryRoot;
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

	public Integer getLocalAutodetectionPeriod() {
		return localAutodetectionPeriod;
	}

	public String getLocalRanges() {
		return localRanges;
	}

	public Integer getWorkPerThread() {
		return workPerThread;
	}

	public Integer getFjpSize() {
		return fjpSize;
	}
	
}
