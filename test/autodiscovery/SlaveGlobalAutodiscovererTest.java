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
import main.AppProperties;

public class SlaveGlobalAutodiscovererTest {

	@Test
	public void testDiscover() {
		SlaveAutodiscoveryScheduler slaveScheduler = mock(SlaveAutodiscoveryScheduler.class);
		IpFJPScanner ipScanner = mock(IpFJPScanner.class);
		AppProperties appProperties = mock(AppProperties.class); 
		
		when(slaveScheduler.checkAndUpdateBaseTime(2)).thenReturn(true);
		when(slaveScheduler.isScheduled(2)).thenReturn(true);
		when(appProperties.getGlobalRanges()).thenReturn("109.185.0.0/16");
		when(ipScanner.scan("109.185.0.0/16")).thenReturn(List.of("109.185.0.13", "109.185.0.14"));
		
		ArgumentCaptor<Integer> arg1 = ArgumentCaptor.forClass(Integer.class);
		
		SlaveGlobalAutodiscoverer sla = new SlaveGlobalAutodiscoverer(slaveScheduler, 
																	ipScanner, 
																	appProperties);
		sla.discover(2);
		
		verify(slaveScheduler).checkAndUpdateBaseTime(arg1.capture());
		verify(slaveScheduler, times(1)).updateBaseTime();
		
		Integer v = arg1.getValue();
		
		assertEquals(2, v);
		
		List<MemberDescriptor> mds = sla.getMds();
		
		assertAll("mds",
				  () -> assertEquals(2, mds.size()),
				  () -> assertEquals(null, mds.get(0).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, mds.get(0).getMemberType()),
				  () -> assertEquals("109.185.0.13", mds.get(0).getIpAddress()),
				  () -> assertEquals(null, mds.get(1).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, mds.get(1).getMemberType()),
				  () -> assertEquals("109.185.0.14", mds.get(1).getIpAddress()));
	}
	
}
