package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.InitializationException;

public class Main {

	private static Logger logger = LogManager.getRootLogger();
	
	public static void main(String[] args) throws InitializationException {
		LifecycleCommand command = args.length > 0 ? LifecycleCommand.to(args[0]) : null;
		
		if(command == null) {
			logger.info("[Main] - No command is specified. Exit.");
			return;
		}
		
		AppProperties appProperties = new AppProperties();
		
		try {
	      Thread.sleep(30_000);
	    } catch (InterruptedException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	    }
		
		if(LifecycleCommand.START == command) {
			//Application startup
			AppContext context = new AppContext();
			context.start(appProperties);
		}
		
		//NOT IMPLEMENTED
		if(LifecycleCommand.STOP == command) {
			//Application shutdown
			new ShutdownSignal(appProperties).send();
		}
	}

}