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

	public AppInitializer(BaseRepositoryOperations bro) {
		super();
		this.bro = bro;
	}
	
	/**
	 * Checks if sys directory exists or creates new one otherwise
	 */
	public void initSysDirectory() {
		try {
			//check sys directory existence
			Path sys = Paths.get(".sys");
			bro.createDirectoryIfNotExist(sys);
			bro.hideDirectory(sys);
			
			//check nodes file existence
			Path nodes = sys.resolve(Paths.get("nodes.txt"));
			bro.createFileIfNotExist(nodes);
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] unable to create sys file", e);
		} 
	}
	
}
