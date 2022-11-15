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
	
	private SourceMemberAutodiscoverer sma;
	
	private CloudMemberAutodiscoverer cma;
	
	private volatile Thread localT;
	
	private volatile Thread globalT;
	
	public IpAutodiscoverer(MemberIpMonitor mim, 
							SourceMemberAutodiscoverer sma,
							CloudMemberAutodiscoverer cma) {
		this.mim = mim;
		this.sma = sma;
		this.cma = cma;
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
			MemberDescriptor mdSource = sma.getMemberDescriptor();
			
			if(!mim.isActiveSourceMember() && mdSource != null) {
				mim.setSourceIp(mdSource.getMemberId(), mdSource.getIpAddress());
			}
		}
		
		if(localT == null && !mim.isActiveSourceMember()) {
			boolean ipFound = mim.sourceFailureCounter() != -1;
			IpContext ic = new IpContext(ipFound,
										 mim.sourceFailureCounter());
			localT = new Thread(() -> sma.discover(ic));
			localT.setName(sma.getClass().getSimpleName());
			localT.start();
		}
	}
	
	private void runGlobally() throws IOException {
		// previously started global autodiscovery thread finished
		//TODO: this is the wrong approach to rely on thread state. Review it
		if(globalT != null && State.TERMINATED.equals(globalT.getState())) {
			globalT = null;
			List<MemberDescriptor> mds = cma.getMds();
			
			if(!mim.areActiveCloudMembers() && mds != null && mds.size() > 0) {
				mim.setCloudIps(mds);
			}
		}
		
		if(globalT == null && !mim.areActiveCloudMembers()) {
			boolean allIpsFound = mim.areAllCloudMembersInitialized();
			IpContext ic = new IpContext(allIpsFound,
										 mim.cloudFailureCounter());
			globalT = new Thread(() -> cma.discover(ic));
			globalT.setName(cma.getClass().getSimpleName());
			globalT.start();
		}
	}
	
}
