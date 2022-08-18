package autodiscovery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import autodiscovery.ipscanner.IpFJPScanner;
import autodiscovery.ipscanner.IpScannerResult;
import main.AppProperties;

public class SlaveLocalAutodiscovererTest {

	@Test
	public void testDiscover() {
		MemberAutodiscoveryScheduler scheduler = mock(MemberAutodiscoveryScheduler.class);
		IpFJPScanner ipScanner = mock(IpFJPScanner.class);
		MemberIpMonitor memberIpMonitor = mock(MemberIpMonitor.class);
		AppProperties appProperties = mock(AppProperties.class); 
		
		when(scheduler.checkAndUpdateBaseTime(2)).thenReturn(true);
		when(scheduler.isScheduled(2)).thenReturn(true);
		when(appProperties.getLocalRanges()).thenReturn("192.168.0.0/24");
		when(appProperties.getMemberId()).thenReturn("member2");
		when(ipScanner.scan("192.168.0.0/24")).thenReturn(
				List.of(new IpScannerResult("192.168.0.13", "member1")));
		when(memberIpMonitor.memberTypeByMemberId("member1")).thenReturn(MemberType.SOURCE);
		
		ArgumentCaptor<Integer> arg1 = ArgumentCaptor.forClass(Integer.class);
		
		SourceMemberAutodiscoverer sma = new SourceMemberAutodiscoverer(scheduler, 
																		ipScanner, 
																		memberIpMonitor,
																		appProperties);
		sma.discover(2);
		
		verify(scheduler).checkAndUpdateBaseTime(arg1.capture());
		verify(scheduler, times(1)).updateBaseTime();
		
		Integer v = arg1.getValue();
		
		assertEquals(2, v);
		
		MemberDescriptor md = sma.getMemberDescriptor();
		
		assertAll("md",
				  () -> assertEquals("member1", md.getMemberId()),
				  () -> assertEquals(MemberType.SOURCE, md.getMemberType()),
				  () -> assertEquals("192.168.0.13", md.getIpAddress()));
	}
	
}
