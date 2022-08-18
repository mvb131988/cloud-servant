package autodiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.Thread.State;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exception.NotUniqueSourceMemberException;
import exception.WrongSourceMemberId;

public class IpAutodiscovererTest {

	@Test
	public void testRunLocally() throws IllegalArgumentException, 
									  	IllegalAccessException, 
									  	NoSuchFieldException, 
									  	SecurityException, 
									  	NoSuchMethodException, 
									  	InvocationTargetException,
									  	NotUniqueSourceMemberException, 
									  	InterruptedException, 
									  	WrongSourceMemberId, 
									  	IOException
	{
		// init
		MemberIpMonitor mim = mock(MemberIpMonitor.class);
		SourceMemberAutodiscoverer sma = mock(SourceMemberAutodiscoverer.class);
		IpAutodiscoverer discoverer = new IpAutodiscoverer(mim, sma, null);
		
		when(mim.isActiveSourceMember()).thenReturn(false);
		when(mim.sourceFailureCounter()).thenReturn(3);
		when(sma.getMemberDescriptor()).thenReturn(
				new MemberDescriptor("member2", MemberType.SOURCE, "192.168.0.13"));
		
		//
		
		// first invocation starts autodiscovery thread
		ArgumentCaptor<Integer> arg1= ArgumentCaptor.forClass(Integer.class);
		invokePrivateMethod(discoverer, "runLocally");
		
		Thread localT = (Thread) getPrivateFieldValue(discoverer, "localT");
		assertFalse(null == localT);
		localT.join();
		assertEquals(State.TERMINATED, localT.getState());
		
		verify(sma, times(1)).discover(arg1.capture());
		assertEquals(3, arg1.getValue());
		//
		
		// second invocation starts autodiscovery thread
		ArgumentCaptor<String> arg2= ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg3= ArgumentCaptor.forClass(String.class);
		invokePrivateMethod(discoverer, "runLocally");
		
		verify(mim, times(1)).setSourceIp(arg2.capture(), arg3.capture());
		assertEquals("member2", arg2.getValue());
		assertEquals("192.168.0.13", arg3.getValue());
		//
		
		localT = (Thread) getPrivateFieldValue(discoverer, "localT");
		localT.join();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testRunGlobally() throws IllegalArgumentException, 
									  	 IllegalAccessException, 
									  	 NoSuchFieldException, 
									  	 SecurityException, 
									  	 NoSuchMethodException, 
									  	 InvocationTargetException,
									  	 NotUniqueSourceMemberException, 
									  	 InterruptedException, 
									  	 WrongSourceMemberId, 
									  	 IOException
	{
		// init
		MemberIpMonitor mim = mock(MemberIpMonitor.class);
		CloudMemberAutodiscoverer cma = mock(CloudMemberAutodiscoverer.class);
		IpAutodiscoverer discoverer = new IpAutodiscoverer(mim, null, cma);
		
		List<MemberDescriptor> mds0 = new ArrayList<>();
		mds0.add(new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.13"));
		
		when(mim.areActiveCloudMembers()).thenReturn(false);
		when(mim.cloudFailureCounter()).thenReturn(3);
		when(cma.getMds()).thenReturn(mds0);
		
		//
		
		// first invocation starts autodiscovery thread
		ArgumentCaptor<Integer> arg1= ArgumentCaptor.forClass(Integer.class);
		invokePrivateMethod(discoverer, "runGlobally");
		
		Thread globalT = (Thread) getPrivateFieldValue(discoverer, "globalT");
		assertFalse(null == globalT);
		globalT.join();
		assertEquals(State.TERMINATED, globalT.getState());
		
		verify(cma, times(1)).discover(arg1.capture());
		assertEquals(3, arg1.getValue());
		//
		
		// second invocation starts autodiscovery thread
		ArgumentCaptor<List> arg2= ArgumentCaptor.forClass(List.class);
		invokePrivateMethod(discoverer, "runGlobally");
		
		verify(mim, times(1)).setCloudIps(arg2.capture());
		List<MemberDescriptor> mds = arg2.getValue();
		assertEquals(1, mds.size());
		assertEquals("member2", mds.get(0).getMemberId());
		assertEquals(MemberType.CLOUD, mds.get(0).getMemberType());
		assertEquals("192.168.0.13", mds.get(0).getIpAddress());
		//
		
		globalT = (Thread) getPrivateFieldValue(discoverer, "globalT");
		globalT.join();
	}
	
	private void invokePrivateMethod(Object o, String fName) 
			throws IllegalArgumentException, 
				   IllegalAccessException, 
				   NoSuchFieldException, 
				   SecurityException, 
				   NoSuchMethodException, 
				   InvocationTargetException 
	{
		Method m = o.getClass().getDeclaredMethod(fName);
		m.setAccessible(true);
		m.invoke(o);
		m.setAccessible(false);
	}
	
	private void setPrivateFieldValue(Object o, String fName, Object v) 
			throws IllegalArgumentException,
				   IllegalAccessException,
				   NoSuchFieldException,
				   SecurityException
	{
		Field f = o.getClass().getDeclaredField(fName);
		f.setAccessible(true);
		f.set(o, v);
		f.setAccessible(false);
	}
	
	private Object getPrivateFieldValue(Object o, String fName) 
			throws IllegalArgumentException, 
				   IllegalAccessException, 
				   NoSuchFieldException, 
				   SecurityException 
	{
		Field f = o.getClass().getDeclaredField(fName);
		f.setAccessible(true);
		Object v = f.get(o);
		f.setAccessible(false);
		return v;
	}
}
