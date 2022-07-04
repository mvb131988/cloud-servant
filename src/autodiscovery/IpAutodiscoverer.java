package autodiscovery;

import java.io.IOException;
import java.lang.Thread.State;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.NotUniqueSourceMemberException;
import exception.WrongSourceMemberId;

public class IpAutodiscoverer implements Runnable {

	private Logger logger = LogManager.getRootLogger();
	
	private MemberIpMonitor mim;
	
	private SlaveLocalAutodiscoverer sla;
	
	private SlaveGlobalAutodiscoverer sga;
	
	private Thread localT;
	
	private Thread globalT;
	
	public IpAutodiscoverer(MemberIpMonitor mim, 
							SlaveLocalAutodiscoverer sla,
							SlaveGlobalAutodiscoverer sga) {
		this.mim = mim;
		this.sla = sla;
		this.sga = sga;
	}
	
	@Override
	public void run() {
		logger.info("Started ip autodiscover thread");
		
		boolean fatalError = false;
		for(;;) {
			try {
				if(!fatalError) {
					runLocally();
					runGlobally();
				} else {
					logger.error("Fatal error. System in error state. Termination is required");
				}
				
				//TODO: timeout to property file
				Thread.sleep(1000);
				
			} catch (NotUniqueSourceMemberException | 
					 WrongSourceMemberId | 
					 InterruptedException | 
					 IOException e) 
			{
				logger.error("Fatal error. System in error state. Termination is required", e);
				fatalError = true;
			}
		}
	}
	
	private void runLocally() throws NotUniqueSourceMemberException, 
									 WrongSourceMemberId, 
									 IOException 
	{
		if(localT == null || State.TERMINATED.equals(localT.getState())) {	
			
			// previously started local autodiscovery thread finished
			if(localT != null) {
				localT = null;
				MemberDescriptor mdSource = sla.getMemberDescriptor();
				
				if(!mim.isActiveSourceMember() && mdSource != null) {
					mim.setSourceIp(mdSource.getMemberId(), mdSource.getIpAddress());
				}
			}
			
			if(!mim.isActiveSourceMember()) {
				int failureCounter = mim.sourceFailureCounter();
				localT = new Thread(() -> sla.discover(failureCounter));
				localT.start();
			}
		}
	}
	
	private void runGlobally() {
		if(globalT == null || State.TERMINATED.equals(globalT.getState())) {
			//try to get the result from sga and set globalT to null
			//update CLOUD ip
			
			// TODO:
			// 1 there is at least one CLOUD member with null ip adress or
			//	 at least one CLOUD exceeds failure counter limit 
			//   (here get the biggest failure counter across all the members)
			// 2 get failure counter from member ip monitor
			if(mim.existEmptyIp()) {
				globalT = new Thread(() -> sga.discover(0));
				globalT.start();
			}
		}
	}
	
}
