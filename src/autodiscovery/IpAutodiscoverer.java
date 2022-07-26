package autodiscovery;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.List;

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
		// previously started local autodiscovery thread finished
		if(localT != null && State.TERMINATED.equals(localT.getState())) {
			localT = null;
			MemberDescriptor mdSource = sla.getMemberDescriptor();
			
			if(!mim.isActiveSourceMember() && mdSource != null) {
				mim.setSourceIp(mdSource.getMemberId(), mdSource.getIpAddress());
			}
		}
		
		if(localT == null && !mim.isActiveSourceMember()) {
			int failureCounter = mim.sourceFailureCounter();
			localT = new Thread(() -> sla.discover(failureCounter));
			localT.setName(sla.getClass().getSimpleName());
			localT.start();
		}
	}
	
	private void runGlobally() throws IOException {
		// previously started global autodiscovery thread finished
		if(globalT != null && State.TERMINATED.equals(globalT.getState())) {
			globalT = null;
			List<MemberDescriptor> mds = sga.getMds();
			
			if(!mim.areActiveCloudMembers() && mds != null && mds.size() > 0) {
				mim.setCloudIps(mds);
			}
		}
		
		if(globalT == null && !mim.areActiveCloudMembers()) {
			int failureCounter = mim.cloudFailureCounter();
			globalT = new Thread(() -> sga.discover(failureCounter));
			globalT.setName(sga.getClass().getSimpleName());
			globalT.start();
		}
	}
	
}
