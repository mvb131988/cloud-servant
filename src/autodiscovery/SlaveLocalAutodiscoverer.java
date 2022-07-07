package autodiscovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpValidator;
import main.AppProperties;
import transfer.context.StatusTransferContext;

/**
 * Intended for SOURCE member autodiscovering when SOURCE member and CLOUD member run in the
 * same local network. SOURCE member is going to be discovered by CLOUD member 
 */
public class SlaveLocalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private String localRanges;
	
	private MemberDescriptor md;
	
	private IpValidator ipValidator;
	
	public SlaveLocalAutodiscoverer(SlaveAutodiscoveryScheduler slaveScheduler, 
									IpFJPScanner ipScanner,
									IpValidator ipValidator,
									AppProperties ap) 
	{
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.ipValidator = ipValidator;
		this.localRanges = ap.getLocalRanges();
	}
	
	//TODO: no return type
	//		change failureCounter to requestScan
	//		when requestScan set check if scan timeout is reached and initiate new scan
	@Override
	public List<String> discover(int failureCounter) {
		List<String> sourceIps = new ArrayList<String>();
		md = null;
		
		// Local autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isLocalScheduled = slaveScheduler.isScheduled(failureCounter);
		
		if(isLocalScheduled) {
			logger.info("[" + this.getClass().getSimpleName() + "] local scan start");

			//no filtering, all candidates in local network are supposed to be valid 
			//cloud-servant members
			sourceIps.addAll(ipScanner.scan(localRanges));
			
			StatusTransferContext stc = ipValidator.isValid(sourceIps.get(0));
			
			// TODO: get member type by member id here
			// at most one SOURCE member is allowed in local autodiscovery scan 
			if(sourceIps.size() == 1) {
				md = new MemberDescriptor(stc.getMemberId(), MemberType.SOURCE, sourceIps.get(0));
			}
			
			sourceIps.stream().forEach(
			    sourceIp -> logger.info("[" + this.getClass().getSimpleName() + "] "
			                + "local scan finish with source ip = " + sourceIp)
			);
			
			slaveScheduler.updateBaseTime();
		} 
		
		return null;
	}

	public MemberDescriptor getMemberDescriptor() {
		return md;
	}

}
