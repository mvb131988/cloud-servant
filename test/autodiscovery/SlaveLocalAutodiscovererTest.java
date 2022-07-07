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
import autodiscovery.ipscanner.IpValidator;
import main.AppProperties;
import transfer.context.StatusTransferContext;

public class SlaveLocalAutodiscovererTest {

	@Test
	public void testDiscover() {
		SlaveAutodiscoveryScheduler slaveScheduler = mock(SlaveAutodiscoveryScheduler.class);
		IpFJPScanner ipScanner = mock(IpFJPScanner.class);
		IpValidator ipValidator = mock(IpValidator.class);
		AppProperties appProperties = mock(AppProperties.class); 
		
		when(slaveScheduler.checkAndUpdateBaseTime(2)).thenReturn(true);
		when(slaveScheduler.isScheduled(2)).thenReturn(true);
		when(appProperties.getLocalRanges()).thenReturn("192.168.0.0/24");
		when(ipScanner.scan("192.168.0.0/24")).thenReturn(List.of("192.168.0.13"));
		when(ipValidator.isValid("192.168.0.13"))
			.thenReturn(new StatusTransferContext(null, "member1"));
		
		ArgumentCaptor<Integer> arg1 = ArgumentCaptor.forClass(Integer.class);
		
		SlaveLocalAutodiscoverer sla = new SlaveLocalAutodiscoverer(slaveScheduler, 
																	ipScanner, 
																	ipValidator,
																	appProperties);
		sla.discover(2);
		
		verify(slaveScheduler).checkAndUpdateBaseTime(arg1.capture());
		verify(slaveScheduler, times(1)).updateBaseTime();
		
		Integer v = arg1.getValue();
		
		assertEquals(2, v);
		
		MemberDescriptor md = sla.getMemberDescriptor();
		
		assertAll("md",
				  () -> assertEquals("member1", md.getMemberId()),
				  () -> assertEquals(MemberType.SOURCE, md.getMemberType()),
				  () -> assertEquals("192.168.0.13", md.getIpAddress()));
	}
	
}
