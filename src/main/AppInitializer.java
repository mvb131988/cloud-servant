package main;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.BaseRepositoryOperations;

/**
 * Initializes application working directories
 */
public class AppInitializer {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;

	private Path pathRoot;
	
	private Path pathSys;
	
	private Path pathLog;
	
	private Path pathTemp;
	
	private String memberId;
	
	public AppInitializer(BaseRepositoryOperations bro, AppProperties appProperties) {
		super();
		this.bro = bro;
		this.pathRoot = appProperties.getRepositoryRoot();
		this.pathSys = appProperties.getPathSys();
		this.pathLog = appProperties.getPathLog();
		this.pathTemp = appProperties.getPathTemp();
		this.memberId = appProperties.getMemberId();
	}
	
	/**
	 * Checks if sys directory exists or creates new one otherwise
	 */
	public void initSysDirectory() {
		try {
			logger.info("System path is: " + pathSys);
			
			//check sys directory existence
			bro.createDirectoryIfNotExistA(pathSys);
			bro.hideDirectory(pathSys);
			
			logger.info("Log path is: " + pathLog);
			
			//check log directory existence
			bro.createDirectoryIfNotExistA(pathLog);
			bro.hideDirectory(pathLog);
			
			logger.info("Temp path is: " + pathTemp);
			
			//check log directory existence
			bro.createDirectoryIfNotExistA(pathTemp);
			bro.hideDirectory(pathTemp);
			
			//existed file is replaced by an empty one
			Path configPath = pathRoot.resolve(memberId + "_data.repo");
			try (OutputStream os = Files.newOutputStream(configPath);) {

			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "]"
						   + " unable to create " + memberId + "_data.repo", e);
			} 
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() 
				       + "] unable to create sys file", e);
		} 
	}
	
}
