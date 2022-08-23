package autodiscovery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpScannerResult;
import main.AppProperties;

public class CloudMemberAutodiscovererTest {

	@Test
	public void testDiscover() {
		MemberAutodiscoveryScheduler scheduler = mock(MemberAutodiscoveryScheduler.class);
		IpFJPScanner ipScanner = mock(IpFJPScanner.class);
		MemberIpMonitor memberIpMonitor = mock(MemberIpMonitor.class);
		AppProperties appProperties = mock(AppProperties.class); 
		IpContext ipContext = mock(IpContext.class);
		
		when(scheduler.isScheduled(ipContext)).thenReturn(true);
		when(appProperties.getGlobalRanges()).thenReturn("109.185.0.0/16");
		when(appProperties.getMemberId()).thenReturn("member3");
		when(ipScanner.scan("109.185.0.0/16")).thenReturn(
				List.of(new IpScannerResult("109.185.0.13", "member1"), 
						new IpScannerResult("109.185.0.14", "member2"))
		);
		when(memberIpMonitor.memberTypeByMemberId("member1")).thenReturn(MemberType.CLOUD);
		when(memberIpMonitor.memberTypeByMemberId("member2")).thenReturn(MemberType.CLOUD);
		
		CloudMemberAutodiscoverer cma = new CloudMemberAutodiscoverer(scheduler, 
																	  ipScanner, 
																	  memberIpMonitor,
																	  appProperties);
		cma.discover(ipContext);
		
		verify(scheduler, times(1)).updateBaseTime();
		
		List<MemberDescriptor> mds = cma.getMds();
		
		assertAll("mds",
				  () -> assertEquals(2, mds.size()),
				  () -> assertEquals("member1", mds.get(0).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, mds.get(0).getMemberType()),
				  () -> assertEquals("109.185.0.13", mds.get(0).getIpAddress()),
				  () -> assertEquals("member2", mds.get(1).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, mds.get(1).getMemberType()),
				  () -> assertEquals("109.185.0.14", mds.get(1).getIpAddress()));
	}
	
}
