package repository;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service layer to manage sys directory and containing files 
 */
public class SysManager {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;
	
	public SysManager(BaseRepositoryOperations bro) {
		super();
		this.bro = bro;
	}
	
	public void persistMasterIp(String ip) {
		try {
			bro.writeMasterIp(ip);
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] fails to persist master ip ", e); 
		}
	}
	
	public String getMasterIp() {
		String ip = null;
		try {
			ip = bro.readMasterIp();
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] fails to get master ip ", e); 
		}
		return ip;
	}
	
}
