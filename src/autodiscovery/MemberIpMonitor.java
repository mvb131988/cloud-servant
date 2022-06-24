package autodiscovery;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.InitializationException;
import main.AppProperties;
import repository.BaseRepositoryOperations;

public class MemberIpMonitor {

	private Logger logger = LogManager.getRootLogger();

	private BaseRepositoryOperations bro;
	
	// local member id
	private String memberId;
	
	// list of all external members (their configuration including ip) in the cluster  
	private List<MemberDescriptor> ds;
	
	public MemberIpMonitor(BaseRepositoryOperations bro, AppProperties app) 
			throws InitializationException 
	{
		this.bro = bro;
		
		memberId = null;
		//member descriptors without ip addresses (static configuration)
		List<MemberDescriptor> ds0 = null;
		try {
			memberId = bro.loadLocalMemberId("members.properties");
			ds0 = bro.loadRemoteMembers("members.properties");
		} catch (IOException ex) {
			logger.error("Exception during members property (static file) file read", ex);
			throw new InitializationException("Exception during members property (static file) "
					+ "file read" , ex);
		}
		
		//dynamic configuration with ip addresses
		Path pathTxt = app.getPathSys().resolve(Paths.get("members.txt"));
		try {
			bro.createMembersFileIfNotExist(pathTxt, memberId, ds0);
		} catch (IOException ex) {
			logger.error("Exception during members configuration file creation", ex);
			throw new InitializationException("Exception during members configuration "
					+ "file creation", ex);
		}
		
		//load members.txt into internal structure
		try {
			ds = bro.loadRemoteMembers(pathTxt);
		} catch (IOException ex) {
			logger.error("Exception during members txt (dynamic file) file read", ex);
			throw new InitializationException("Exception during members txt (dynamic file) "
					+ "file read", ex);
		}
	}
	
	public boolean existSourceMemberId() {
		return false;
	}
	
	public boolean existEmptyIp() {
		return false;
	}
	
	public void updateIp(String ip) {
		
	}
	
	public List<String> getNotNullIps() {
		return null;
	}
}
