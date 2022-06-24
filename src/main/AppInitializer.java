package main;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import repository.BaseRepositoryOperations;

/**
 * Initializes application working directories
 */
public class AppInitializer {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseRepositoryOperations bro;

	private Path pathSys;
	
	public AppInitializer(BaseRepositoryOperations bro, AppProperties appProperties) {
		super();
		this.bro = bro;
		this.pathSys = appProperties.getPathSys();
	}
	
	/**
	 * Checks if sys directory exists or creates new one otherwise
	 */
	public void initSysDirectory() {
		try {
			logger.info("System path is: " + pathSys);
			
			//check sys directory existence
			bro.createDirectoryIfNotExist0(pathSys);
			bro.hideDirectory(pathSys);
			
			//check nodes file existence
			//TODO: Deprecated substitute nodes.txt with members.txt
			Path nodes = pathSys.resolve(Paths.get("nodes.txt"));
			bro.createFileIfNotExist(nodes);
			////////////////////////////////////////////////////////
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] unable to create sys file", e);
		} 
	}
	
}
