package autodiscovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ipscanner.IpFJPScanner;
import ipscanner.IpValidator;
import main.AppProperties;
import repository.SysManager;

public class SlaveGlobalAutodiscoverer implements Autodiscovery {

	private static Logger logger = LogManager.getRootLogger();
	
	private IpFJPScanner ipScanner; 
	
	private SlaveAutodiscoveryScheduler slaveScheduler;
	
	private String globalRanges;
	
	private SysManager sysManager;
	
	private IpValidator ipValidator;
	
	public SlaveGlobalAutodiscoverer(SlaveAutodiscoveryScheduler slaveScheduler, 
									 IpFJPScanner ipScanner, 
									 SysManager sysManager,
									 IpValidator ipValidator,
									 AppProperties ap) {
		this.slaveScheduler = slaveScheduler;
		this.ipScanner = ipScanner;
		this.sysManager = sysManager;
		this.ipValidator = ipValidator;
		this.globalRanges = ap.getGlobalRanges();
	}
	
	@Override
	public List<String> discover(int failureCounter) {
		//TODO: need to support multiple ips reads, writes
	  List<String> masterIps = new ArrayList<>();
	  masterIps.add(sysManager.getMasterIp());
	  
	  List<String> newMasterIps = new ArrayList<String>();
		
		// Global autodiscovery
		slaveScheduler.checkAndUpdateBaseTime(failureCounter);
		boolean isScheduled = slaveScheduler.isScheduled(failureCounter);
		if(isScheduled) {
			
			logger.info("[" + this.getClass().getSimpleName() + "] global scan start");
			List<String> masterIpCandidates = ipScanner.scan(globalRanges);
			
			masterIpCandidates.stream().forEach(
          mic -> logger.info("[" + this.getClass().getSimpleName() 
                 + "] global scan finished with master ip candidate = " + mic)
      );
			
			//at this moment the list contains only one master ip
			newMasterIps = ipValidator.getValid(masterIpCandidates);
			
			newMasterIps.stream().forEach(
          mi -> logger.info("[" + this.getClass().getSimpleName() 
                + "] found master ip = " + mi)
      );
			
			slaveScheduler.updateBaseTime();
		} 
		
		//TODO: need to support multiple ips reads, writes
		if(newMasterIps.size() > 0) {
		  sysManager.persistMasterIp(newMasterIps.get(0));
		}

		return newMasterIps.size() > 0 ? newMasterIps: masterIps;
	}

}
