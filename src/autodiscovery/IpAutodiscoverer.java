package autodiscovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IpAutodiscoverer implements Runnable {

	private Logger logger = LogManager.getRootLogger();
	
	private MemberIpMonitor mim;
	
	public IpAutodiscoverer(MemberIpMonitor mim) {
		this.mim = mim;
	}
	
	@Override
	public void run() {
		logger.info("Started ip autodiscover thread");
		
		//local autodiscovery
		//read .sys members file and check that exist exactly one memberId of type=SOURCE
		
		//global autodiscovery
		//read .sys members file and check that each memberId (type=CLOUD) pairs with ip address 
	}

}
