package autodiscovery;

import java.time.ZonedDateTime;

/**
 * Is used on any CLOUD member (any CLOUD member is going to discover all other CLOUD members) to
 * schedule autodiscovery process.
 * 
 */
public class MemberAutodiscoveryScheduler {

	private ZonedDateTime baseTime;
	
	// local autodetection period in seconds
	private int autodetectionPeriod;
	
	public MemberAutodiscoveryScheduler(int autodetectionPeriod) {
		this.autodetectionPeriod = autodetectionPeriod;
	}
	
	public boolean isScheduled(IpContext sic) {
		
		// when no ip is found immediately schedule a scan
		if(!sic.areAllIpsFound()) {
			return true;
		}
		
		// baseTime must not be null
		if(sic.getFailureCounter() > 1 && 
				ZonedDateTime.now().toInstant().isAfter(
						baseTime.plusSeconds(autodetectionPeriod).toInstant())) {
			return true;
		}
		
		return false;
	}

	/**
	 * Update base time after autodetection process is terminated.
	 */
	public void updateBaseTime() {
		baseTime = ZonedDateTime.now().plusSeconds(autodetectionPeriod);
	}
	
}
