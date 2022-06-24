package autodiscovery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import main.AppProperties;
import repository.BaseRepositoryOperations;

public class MemberIpMonitorTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor1() throws IOException, 
										 IllegalArgumentException, 
										 IllegalAccessException, 
										 NoSuchFieldException, 
										 SecurityException 
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
		List<MemberDescriptor> actualDs = (List<MemberDescriptor>) f.get(mim);
		f.setAccessible(false);
		
		assertEquals(ds, actualDs);
		
		assertAll("ds",
				  () -> assertEquals(ds.size(), actualDs.size()),
				  () -> assertEquals(ds.get(0).getMemberId(), actualDs.get(0).getMemberId()),
				  () -> assertEquals(ds.get(0).getMemberType(), actualDs.get(0).getMemberType()),
				  () -> assertEquals(ds.get(0).getIpAddress(), actualDs.get(0).getIpAddress()),
				  () -> assertEquals(ds.get(1).getMemberId(), actualDs.get(1).getMemberId()),
				  () -> assertEquals(ds.get(1).getMemberType(), actualDs.get(1).getMemberType()),
				  () -> assertEquals(ds.get(2).getMemberId(), actualDs.get(2).getMemberId()),
				  () -> assertEquals(ds.get(2).getMemberType(), actualDs.get(2).getMemberType()),
				  () -> assertEquals(ds.get(3).getMemberId(), actualDs.get(3).getMemberId()),
				  () -> assertEquals(ds.get(3).getMemberType(), actualDs.get(3).getMemberType()));
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
	
}
