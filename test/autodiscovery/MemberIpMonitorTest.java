package autodiscovery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exception.InitializationException;
import exception.NotUniqueSourceMemberException;
import exception.WrongSourceMemberId;
import main.AppProperties;
import repository.BaseRepositoryOperations;

public class MemberIpMonitorTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor1() throws IOException, 
										 IllegalArgumentException, 
										 IllegalAccessException, 
										 NoSuchFieldException, 
										 SecurityException, 
										 InitializationException 
	{
		AppProperties appProperties = mock(AppProperties.class);
		when(appProperties.getPathSys()).thenReturn(Paths.get(""));
		
		List<MemberDescriptor> ds0 = new ArrayList<>();
		ds0.add(new MemberDescriptor("member2", MemberType.CLOUD, null));
		ds0.add(new MemberDescriptor("member3", MemberType.CLOUD, null));
		ds0.add(new MemberDescriptor("member4", MemberType.CLOUD, null));
		ds0.add(new MemberDescriptor("member5", MemberType.SOURCE, null));
		
		List<MemberDescriptor> ds = new ArrayList<>();
		ds.add(new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.13"));
		ds.add(new MemberDescriptor("member3", MemberType.CLOUD, null));
		ds.add(new MemberDescriptor("member4", MemberType.CLOUD, null));
		ds.add(new MemberDescriptor("member5", MemberType.SOURCE, null));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		when(bro.loadLocalMemberId("members.properties")).thenReturn("member1");
		when(bro.loadRemoteMembers("members.properties")).thenReturn(ds0);
		when(bro.loadRemoteMembers(Paths.get("members.txt"))).thenReturn(ds);
		
		ArgumentCaptor<Path> arg1= ArgumentCaptor.forClass(Path.class);
		ArgumentCaptor<String> arg2= ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List<MemberDescriptor>> arg3= ArgumentCaptor.forClass(List.class);
		
		MemberIpMonitor mim = new MemberIpMonitor(bro, appProperties);
		
		verify(bro, times(1)).createMembersFileIfNotExist(arg1.capture(), 
														  arg2.capture(), 
														  arg3.capture());
		Path actualPath = arg1.getValue();
		String actualMemberId = arg2.getValue();
		List<MemberDescriptor> actualDs0 = arg3.getValue();
		
		assertAll("createMembersFileIfNotExist",
				  () -> assertEquals(Paths.get("members.txt"), actualPath),
				  () -> assertEquals("member1", actualMemberId),
				  () -> assertEquals(ds0.size(), actualDs0.size()),
				  () -> assertEquals(ds0.get(0).getMemberId(), actualDs0.get(0).getMemberId()),
				  () -> assertEquals(ds0.get(0).getIpAddress(), actualDs0.get(0).getIpAddress()),
				  () -> assertEquals(ds0.get(1).getMemberId(), actualDs0.get(1).getMemberId()),
				  () -> assertEquals(ds0.get(1).getMemberType(), actualDs0.get(1).getMemberType()),
				  () -> assertEquals(ds0.get(2).getMemberId(), actualDs0.get(2).getMemberId()),
				  () -> assertEquals(ds0.get(2).getMemberType(), actualDs0.get(2).getMemberType()),
				  () -> assertEquals(ds0.get(3).getMemberId(), actualDs0.get(3).getMemberId()),
				  () -> assertEquals(ds0.get(3).getMemberType(), 
						  actualDs0.get(3).getMemberType()));
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		List<EnhancedMemberDescriptor> actualDs = (List<EnhancedMemberDescriptor>) f.get(mim);
		f.setAccessible(false);
		
		assertAll("ds",
				  () -> assertEquals(ds.size(), actualDs.size()),
				  () -> assertEquals(ds.get(0).getMemberId(), 
						  			 actualDs.get(0).getMd().getMemberId()),
				  () -> assertEquals(ds.get(0).getMemberType(), 
						  			 actualDs.get(0).getMd().getMemberType()),
				  () -> assertEquals(ds.get(0).getIpAddress(), 
						  			 actualDs.get(0).getMd().getIpAddress()),
				  () -> assertEquals(ds.get(1).getMemberId(), 
						  			 actualDs.get(1).getMd().getMemberId()),
				  () -> assertEquals(ds.get(1).getMemberType(), 
						  			 actualDs.get(1).getMd().getMemberType()),
				  () -> assertEquals(ds.get(2).getMemberId(), 
						  			 actualDs.get(2).getMd().getMemberId()),
				  () -> assertEquals(ds.get(2).getMemberType(), 
						  			 actualDs.get(2).getMd().getMemberType()),
				  () -> assertEquals(ds.get(3).getMemberId(), 
						  			 actualDs.get(3).getMd().getMemberId()),
				  () -> assertEquals(ds.get(3).getMemberType(), 
						  			 actualDs.get(3).getMd().getMemberType()));
	}
	
	@Test
	public void testConstructor2() throws IOException {
		AppProperties appProperties = mock(AppProperties.class);
		when(appProperties.getPathSys()).thenReturn(Paths.get(""));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		when(bro.loadLocalMemberId("members.properties")).thenThrow(new IOException());
		
		InitializationException thrown = assertThrows(InitializationException.class, 
				() -> { new MemberIpMonitor(bro, appProperties); });
		assertEquals("Exception during members property (static file) file read", 
				thrown.getMessage());
		
		verify(bro, times(0)).createMembersFileIfNotExist(any(), any(), any());
		verify(bro, times(0)).loadRemoteMembers(any(Path.class));
	}
	
	@Test
	public void testConstructor3() throws IOException {
		AppProperties appProperties = mock(AppProperties.class);
		when(appProperties.getPathSys()).thenReturn(Paths.get(""));
		
		List<MemberDescriptor> ds0 = new ArrayList<>();
		ds0.add(new MemberDescriptor("member2", MemberType.CLOUD, null));
		ds0.add(new MemberDescriptor("member3", MemberType.CLOUD, null));
		ds0.add(new MemberDescriptor("member4", MemberType.CLOUD, null));
		ds0.add(new MemberDescriptor("member5", MemberType.SOURCE, null));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		when(bro.loadLocalMemberId("members.properties")).thenReturn("member1");
		when(bro.loadRemoteMembers("members.properties")).thenReturn(ds0);
		
		doThrow(new IOException()).when(bro).createMembersFileIfNotExist(
				Paths.get("members.txt"), "member1", ds0);
		
		InitializationException thrown = assertThrows(InitializationException.class, 
				() -> { new MemberIpMonitor(bro, appProperties); });
		assertEquals("Exception during members configuration file creation", 
				thrown.getMessage());
		
		verify(bro, times(0)).loadRemoteMembers(any(Path.class));
	}
	
	@Test
	public void testConstructor4() throws IOException {
		AppProperties appProperties = mock(AppProperties.class);
		when(appProperties.getPathSys()).thenReturn(Paths.get(""));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		when(bro.loadRemoteMembers(Paths.get("members.txt"))).thenThrow(new IOException());
		
		InitializationException thrown = assertThrows(InitializationException.class, 
				() -> { new MemberIpMonitor(bro, appProperties); });
		assertEquals("Exception during members txt (dynamic file) file read", 
				thrown.getMessage());
	}
	
	@Test
	public void testExistSourceMemberId1() throws InitializationException, 
												 NoSuchFieldException, 
												 SecurityException, 
												 IllegalArgumentException, 
												 IllegalAccessException, 
												 NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		assertFalse(mim.isActiveSourceMember());
	}
	
	@Test
	public void testExistSourceMemberId2() throws InitializationException, 
												 NoSuchFieldException, 
												 SecurityException, 
												 IllegalArgumentException, 
												 IllegalAccessException, 
												 NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, "192.168.0.13"), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		assertTrue(mim.isActiveSourceMember());
	}
	
	@Test
	public void testExistSourceMemberId3() throws InitializationException, 
												 NoSuchFieldException, 
												 SecurityException, 
												 IllegalArgumentException, 
												 IllegalAccessException, 
												 NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, "192.168.0.13"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member6", MemberType.SOURCE, "192.168.0.11"), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		assertThrows(NotUniqueSourceMemberException.class, () -> mim.isActiveSourceMember());
	}
	
	@Test
	public void testAreActiveCloudMembers1() throws InitializationException, 
													NoSuchFieldException, 
													SecurityException, 
													IllegalArgumentException, 
													IllegalAccessException, 
													NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.11"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, "192.168.0.12"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, "192.168.0.13"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		assertTrue(mim.areActiveCloudMembers());
	}
	
	@Test
	public void testAreActiveCloudMembers2() throws InitializationException, 
												    NoSuchFieldException, 
												    SecurityException, 
												    IllegalArgumentException, 
												    IllegalAccessException, 
												    NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.11"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, "192.168.0.13"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		assertFalse(mim.areActiveCloudMembers());
	}
	
	@Test
	public void testSourceFailureCounter1()  throws InitializationException, 
											 NoSuchFieldException, 
											 SecurityException, 
											 IllegalArgumentException, 
											 IllegalAccessException, 
											 NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, "192.168.0.14"), 1));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member6", MemberType.SOURCE, null), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		int counter = mim.sourceFailureCounter();
		assertEquals(1, counter);
	}
	
	@Test
	public void testSourceFailureCounter2()  throws InitializationException, 
												 NoSuchFieldException, 
												 SecurityException, 
												 IllegalArgumentException, 
												 IllegalAccessException, 
												 NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		int counter = mim.sourceFailureCounter();
		assertEquals(-1, counter);
	}
	
	@Test
	public void testSourceFailureCounter3() throws InitializationException, 
												 NoSuchFieldException, 
												 SecurityException, 
												 IllegalArgumentException, 
												 IllegalAccessException, 
												 NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, "192.168.0.11"), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member6", MemberType.SOURCE, "192.168.0.13"), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		assertThrows(NotUniqueSourceMemberException.class, () -> mim.sourceFailureCounter());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSetSourceIp1() throws InitializationException, 
										  NoSuchFieldException, 
										  SecurityException, 
										  IllegalArgumentException, 
										  IllegalAccessException, 
										  NotUniqueSourceMemberException, 
										  WrongSourceMemberId, 
										  IOException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("bro");
		f.setAccessible(true);
		f.set(mim, bro);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("memberId");
		f.setAccessible(true);
		f.set(mim, "member1");
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("pathTxt");
		f.setAccessible(true);
		f.set(mim, Paths.get("/path/to/members.txt"));
		f.setAccessible(false);
		
		ArgumentCaptor<Path> arg1= ArgumentCaptor.forClass(Path.class);
		ArgumentCaptor<String> arg2= ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List<MemberDescriptor>> arg3= ArgumentCaptor.forClass(List.class);
		
		mim.setSourceIp("member5", "192.168.0.13");
		
		verify(bro, times(1)).persistMembersDescriptors(arg1.capture(),
														arg2.capture(), 
														arg3.capture());
		List<MemberDescriptor> actualDs = arg3.getValue();
		
		assertEquals(Paths.get("/path/to/members.txt"), arg1.getValue());
		assertEquals("member1", arg2.getValue());
		assertAll("ds",
				  () -> assertEquals(ds.size(), actualDs.size()),
				  () -> assertEquals("member2", actualDs.get(0).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(0).getMemberType()),
				  () -> assertEquals(null, actualDs.get(0).getIpAddress()),
				  () -> assertEquals("member3", actualDs.get(1).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(1).getMemberType()),
				  () -> assertEquals(null, actualDs.get(1).getIpAddress()),
				  () -> assertEquals("member4", actualDs.get(2).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(2).getMemberType()),
				  () -> assertEquals(null, actualDs.get(2).getIpAddress()),
				  () -> assertEquals("member5", actualDs.get(3).getMemberId()),
				  () -> assertEquals(MemberType.SOURCE, actualDs.get(3).getMemberType()),
				  () -> assertEquals("192.168.0.13", actualDs.get(3).getIpAddress()));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSetSourceIp2() throws InitializationException, 
										  NoSuchFieldException, 
										  SecurityException, 
										  IllegalArgumentException, 
										  IllegalAccessException, 
										  NotUniqueSourceMemberException, 
										  WrongSourceMemberId, 
										  IOException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.SOURCE, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("bro");
		f.setAccessible(true);
		f.set(mim, bro);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("memberId");
		f.setAccessible(true);
		f.set(mim, "member1");
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("pathTxt");
		f.setAccessible(true);
		f.set(mim, Paths.get("/path/to/members.txt"));
		f.setAccessible(false);
		
		ArgumentCaptor<Path> arg1= ArgumentCaptor.forClass(Path.class);
		ArgumentCaptor<String> arg2= ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List<MemberDescriptor>> arg3= ArgumentCaptor.forClass(List.class);
		
		mim.setSourceIp("member5", "192.168.0.13");
		
		verify(bro, times(1)).persistMembersDescriptors(arg1.capture(),
														arg2.capture(), 
														arg3.capture());
		List<MemberDescriptor> actualDs = arg3.getValue();
		
		assertEquals(Paths.get("/path/to/members.txt"), arg1.getValue());
		assertEquals("member1", arg2.getValue());
		assertAll("ds",
				  () -> assertEquals(ds.size(), actualDs.size()),
				  () -> assertEquals("member2", actualDs.get(0).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(0).getMemberType()),
				  () -> assertEquals(null, actualDs.get(0).getIpAddress()),
				  () -> assertEquals("member3", actualDs.get(1).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(1).getMemberType()),
				  () -> assertEquals(null, actualDs.get(1).getIpAddress()),
				  () -> assertEquals("member4", actualDs.get(2).getMemberId()),
				  () -> assertEquals(MemberType.SOURCE, actualDs.get(2).getMemberType()),
				  () -> assertEquals(null, actualDs.get(2).getIpAddress()),
				  () -> assertEquals("member5", actualDs.get(3).getMemberId()),
				  () -> assertEquals(MemberType.SOURCE, actualDs.get(3).getMemberType()),
				  () -> assertEquals("192.168.0.13", actualDs.get(3).getIpAddress()));
	}
	
	@Test
	public void testSetSourceIp3() throws InitializationException, 
										  NoSuchFieldException, 
										  SecurityException, 
										  IllegalArgumentException, 
										  IllegalAccessException, 
										  NotUniqueSourceMemberException, 
										  WrongSourceMemberId, 
										  IOException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.SOURCE, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("bro");
		f.setAccessible(true);
		f.set(mim, bro);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("memberId");
		f.setAccessible(true);
		f.set(mim, "member1");
		f.setAccessible(false);
		
		assertThrows(WrongSourceMemberId.class, 
					() -> mim.setSourceIp("member3", "192.168.0.13"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSetCloudIps1()  throws InitializationException, 
										   NoSuchFieldException, 
										   SecurityException, 
										   IllegalArgumentException, 
										   IllegalAccessException, 
										   NotUniqueSourceMemberException, 
										   WrongSourceMemberId, 
										   IOException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("bro");
		f.setAccessible(true);
		f.set(mim, bro);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("memberId");
		f.setAccessible(true);
		f.set(mim, "member1");
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("pathTxt");
		f.setAccessible(true);
		f.set(mim, Paths.get("/path/to/members.txt"));
		f.setAccessible(false);
		
		ArgumentCaptor<Path> arg1= ArgumentCaptor.forClass(Path.class);
		ArgumentCaptor<String> arg2= ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List<MemberDescriptor>> arg3= ArgumentCaptor.forClass(List.class);
		
		List<MemberDescriptor> updateDs = new ArrayList<>();
		updateDs.add(new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.11"));
		updateDs.add(new MemberDescriptor("member4", MemberType.CLOUD, "192.168.0.12"));
		mim.setCloudIps(updateDs);
		
		verify(bro, times(1)).persistMembersDescriptors(arg1.capture(),
														arg2.capture(), 
														arg3.capture());
		List<MemberDescriptor> actualDs = arg3.getValue();
		
		assertEquals(Paths.get("/path/to/members.txt"), arg1.getValue());
		assertEquals("member1", arg2.getValue());
		assertAll("ds",
				  () -> assertEquals(ds.size(), actualDs.size()),
				  () -> assertEquals("member2", actualDs.get(0).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(0).getMemberType()),
				  () -> assertEquals("192.168.0.11", actualDs.get(0).getIpAddress()),
				  () -> assertEquals("member3", actualDs.get(1).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(1).getMemberType()),
				  () -> assertEquals(null, actualDs.get(1).getIpAddress()),
				  () -> assertEquals("member4", actualDs.get(2).getMemberId()),
				  () -> assertEquals(MemberType.CLOUD, actualDs.get(2).getMemberType()),
				  () -> assertEquals("192.168.0.12", actualDs.get(2).getIpAddress()),
				  () -> assertEquals("member5", actualDs.get(3).getMemberId()),
				  () -> assertEquals(MemberType.SOURCE, actualDs.get(3).getMemberType()),
				  () -> assertEquals(null, actualDs.get(3).getIpAddress()));
	}
	
	/**
	 * The only difference with testSetCloudIps1 is that updateDs (list of discovered memberIds)
	 * contains duplicates
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSetCloudIps2()  throws InitializationException, 
										   NoSuchFieldException, 
										   SecurityException, 
										   IllegalArgumentException, 
										   IllegalAccessException, 
										   NotUniqueSourceMemberException, 
										   WrongSourceMemberId, 
										   IOException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, null), 0));
		
		BaseRepositoryOperations bro = mock(BaseRepositoryOperations.class);
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("bro");
		f.setAccessible(true);
		f.set(mim, bro);
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("memberId");
		f.setAccessible(true);
		f.set(mim, "member1");
		f.setAccessible(false);
		
		f = mim.getClass().getDeclaredField("pathTxt");
		f.setAccessible(true);
		f.set(mim, Paths.get("/path/to/members.txt"));
		f.setAccessible(false);
		
		List<MemberDescriptor> updateDs = new ArrayList<>();
		updateDs.add(new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.11"));
		updateDs.add(new MemberDescriptor("member4", MemberType.CLOUD, "192.168.0.12"));
		updateDs.add(new MemberDescriptor("member4", MemberType.CLOUD, "192.168.0.12"));
		
		assertThrows(IllegalStateException.class, () -> mim.setCloudIps(updateDs));
	}
	
	@Test
	public void testCloudFailureCounter1() throws InitializationException, 
											 	  NoSuchFieldException, 
											 	  SecurityException, 
											 	  IllegalArgumentException, 
											 	  IllegalAccessException, 
											 	  NotUniqueSourceMemberException 
	{
		List<EnhancedMemberDescriptor> ds = new ArrayList<>();
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member2", MemberType.CLOUD, "192.168.0.13"), 2));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member3", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member4", MemberType.CLOUD, null), 0));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member5", MemberType.SOURCE, "192.168.0.14"), 1));
		ds.add(new EnhancedMemberDescriptor(
				new MemberDescriptor("member6", MemberType.SOURCE, null), 0));
		
		MemberIpMonitor mim = new MemberIpMonitor();
		
		Field f = mim.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		f.set(mim, ds);
		f.setAccessible(false);
		
		int counter = mim.cloudFailureCounter();
		assertEquals(2, counter);
	}
	
}
