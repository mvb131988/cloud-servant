package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class AppProperties {
	
	private Path repositoryRoot;
	
	private Path pathSys;
	
	private Path pathLog;
	
	private Path pathTemp;
	
	private Integer transferPort;

	@Deprecated
	private Integer adminPort;
	
	private Integer smallPoolingTimeout;
	
	private Integer bigPoolingTimeout;
	
	private Integer socketSoTimeout;
	
	private Integer localAutodetectionPeriod;
	
	private Integer globalAutodetectionPeriod;
	
	private Integer localWorkPerThread;
	
	private Integer globalWorkPerThread;
	
	private Integer fjpSize;
	
	// Autodetection properties
	private String ISP_HOME = "homelocal";
	
	private String ISP_MOLDTELECOM = "moldtelecom";
	
	private String localRanges;
	
	private String globalRanges;
	
	private String memberId;
	
	public AppProperties() {
		repositoryRoot = Paths.get(ResourceBundle.getBundle("app").getString("path.root"));
		pathSys = Paths.get(ResourceBundle.getBundle("app").getString("path.sys"));
		pathLog = Paths.get(ResourceBundle.getBundle("app").getString("path.log"));
		pathTemp = Paths.get(ResourceBundle.getBundle("app").getString("path.temp"));
		memberId = ResourceBundle.getBundle("app").getString("memberId");
		transferPort = Integer.parseInt(
				ResourceBundle.getBundle("app").getString("member.transfer.port"));
		adminPort = Integer.parseInt(ResourceBundle.getBundle("app").getString("admin.port"));
		
		smallPoolingTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.pooling.small"));
		bigPoolingTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.pooling.big"));
		socketSoTimeout = Integer.parseInt(ResourceBundle.getBundle("app").getString("timeout.so.socket"));
		localAutodetectionPeriod = Integer.parseInt(ResourceBundle.getBundle("app").getString("period.local"));
		globalAutodetectionPeriod = Integer.parseInt(ResourceBundle.getBundle("app").getString("period.global"));
		
		localWorkPerThread = Integer.parseInt(ResourceBundle.getBundle("app").getString("local.autodiscovery.unit.value"));
		globalWorkPerThread = Integer.parseInt(ResourceBundle.getBundle("app").getString("global.autodiscovery.unit.value"));
		
		fjpSize = Integer.parseInt(ResourceBundle.getBundle("app").getString("autodiscovery.pool.size"));
		
		localRanges = ResourceBundle.getBundle("ipranges").getString(ISP_HOME);
		globalRanges = ResourceBundle.getBundle("ipranges").getString(ISP_MOLDTELECOM);
	}

	public Path getRepositoryRoot() {
		return repositoryRoot;
	}

	public Integer getTransferPort() {
		return transferPort;
	}

	public Integer getAdminPort() {
		return adminPort;
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

	public Integer getLocalWorkPerThread() {
		return localWorkPerThread;
	}

	public Integer getGlobalWorkPerThread() {
		return globalWorkPerThread;
	}
	
	public Integer getFjpSize() {
		return fjpSize;
	}

	public String getGlobalRanges() {
		return globalRanges;
	}

	public Integer getGlobalAutodetectionPeriod() {
		return globalAutodetectionPeriod;
	}

	public Path getPathSys() {
		return pathSys;
	}
	
	public Path getPathLog() {
		return pathLog;
	}

	public Path getPathTemp() {
		return pathTemp;
	}
	
	public String getMemberId() {
		return memberId;
	}

}
