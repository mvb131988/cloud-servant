package transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exception.WrongOperationException;
import main.AppProperties;
import transfer.TransferManagerStateMonitor.LockType;
import transfer.constant.MasterStatus;

public class InboundTransferManagerTest {

	@Test
	public void testCommunication1() throws InstantiationException, 
										   IllegalAccessException, 
										   IllegalArgumentException, 
										   InvocationTargetException, 
										   InterruptedException, 
										   IOException, 
										   WrongOperationException 
	{
		FullFileTransferOperation ffto = mock(FullFileTransferOperation.class);
		TransferManagerStateMonitor tmsm = mock(TransferManagerStateMonitor.class);
		HealthCheckOperation hco = mock(HealthCheckOperation.class);
		Socket socket = mock(Socket.class);
		OutputStream os = mock(OutputStream.class);
		InputStream is = mock(InputStream.class);
		
        Class<?> innerClass = InboundTransferManager.class.getDeclaredClasses()[0];
        Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object o = constructor.newInstance(socket, tmsm, hco, ffto);
        constructor.setAccessible(false);
        
        when(tmsm.lock(LockType.INBOUND)).thenReturn(true);
		when(socket.getOutputStream()).thenReturn(os);
		when(socket.getInputStream()).thenReturn(is);
		ArgumentCaptor<OutputStream> arg1 = ArgumentCaptor.forClass(OutputStream.class);
		ArgumentCaptor<PushbackInputStream> arg2 = 
				ArgumentCaptor.forClass(PushbackInputStream.class);
		
        Thread th = new Thread((Runnable) o);
        th.start();
        th.join();
        
		verify(hco, never()).executeAsMaster(any(), any(), any());
        verify(ffto, times(1)).executeAsMaster(arg1.capture(), arg2.capture());
        assertEquals(os, arg1.getValue());
        
        verify(tmsm, times(1)).unlock(LockType.INBOUND);
        verify(socket, times(1)).close();
	}
	
	@Test
	public void testCommunication2() throws InstantiationException, 
										   IllegalAccessException, 
										   IllegalArgumentException, 
										   InvocationTargetException, 
										   InterruptedException, 
										   IOException, 
										   WrongOperationException 
	{
		FullFileTransferOperation ffto = mock(FullFileTransferOperation.class);
		TransferManagerStateMonitor tmsm = mock(TransferManagerStateMonitor.class);
		HealthCheckOperation hco = mock(HealthCheckOperation.class);
		Socket socket = mock(Socket.class);
		OutputStream os = mock(OutputStream.class);
		InputStream is = mock(InputStream.class);
		
        Class<?> innerClass = InboundTransferManager.class.getDeclaredClasses()[0];
        Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object o = constructor.newInstance(socket, tmsm, hco, ffto);
        constructor.setAccessible(false);
        
        when(tmsm.lock(LockType.INBOUND)).thenReturn(false);
		when(socket.getOutputStream()).thenReturn(os);
		when(socket.getInputStream()).thenReturn(is);
		ArgumentCaptor<OutputStream> arg1 = ArgumentCaptor.forClass(OutputStream.class);
		ArgumentCaptor<PushbackInputStream> arg2 = 
				ArgumentCaptor.forClass(PushbackInputStream.class);
		ArgumentCaptor<MasterStatus> arg3 = ArgumentCaptor.forClass(MasterStatus.class);
		
        Thread th = new Thread((Runnable) o);
        th.start();
        th.join();
        
		verify(ffto, never()).executeAsMaster(any(), any());
        verify(hco, times(1)).executeAsMaster(arg1.capture(), arg2.capture(), arg3.capture());
        assertEquals(os, arg1.getValue());
        assertEquals(MasterStatus.BUSY, arg3.getValue());
        
        verify(tmsm, times(1)).unlock(LockType.INBOUND);
        verify(socket, times(1)).close();
	}
	
	@Test
	public void testCommunication3() throws InstantiationException, 
										   IllegalAccessException, 
										   IllegalArgumentException, 
										   InvocationTargetException, 
										   InterruptedException, 
										   IOException, 
										   WrongOperationException 
	{
		FullFileTransferOperation ffto = mock(FullFileTransferOperation.class);
		TransferManagerStateMonitor tmsm = mock(TransferManagerStateMonitor.class);
		HealthCheckOperation hco = mock(HealthCheckOperation.class);
		Socket socket = mock(Socket.class);
		
        Class<?> innerClass = InboundTransferManager.class.getDeclaredClasses()[0];
        Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object o = constructor.newInstance(socket, tmsm, hco, ffto);
        constructor.setAccessible(false);
        
        when(tmsm.lock(LockType.INBOUND)).thenReturn(false);
		when(socket.getOutputStream()).thenThrow(new IOException());
		
        Thread th = new Thread((Runnable) o);
        th.start();
        th.join();
        
        verify(hco, never()).executeAsMaster(any(), any(), any());
		verify(ffto, never()).executeAsMaster(any(), any());
        
        verify(tmsm, times(1)).unlock(LockType.INBOUND);
        verify(socket, times(1)).close();
	}
	
	@Test
	public void testInboundTransferManager() throws InstantiationException, 
										   			IllegalAccessException, 
										   			IllegalArgumentException, 
										   			InvocationTargetException, 
										   			InterruptedException, 
										   			IOException, 
										   			WrongOperationException, 
										   			NoSuchFieldException, 
										   			SecurityException 
	{
		HealthCheckOperation hco = mock(HealthCheckOperation.class);
		FullFileTransferOperation ffto = mock(FullFileTransferOperation.class);
		TransferManagerStateMonitor tmsm = mock(TransferManagerStateMonitor.class);
		AppProperties ap = mock(AppProperties.class);
		
		when(tmsm.lock(LockType.INBOUND)).thenReturn(false);
		when(ap.getSocketSoTimeout()).thenReturn(1000);
		when(ap.getMasterPort()).thenReturn(8080);
		
		InboundTransferManager itm = new InboundTransferManager(hco, 
																ffto, 
																tmsm, 
																ap);
		setPrivateFieldValue(itm, "inTesting", true);
		
		Thread th = new Thread(itm);
		th.start();
		
		Socket s = new Socket("localhost", 8080);
		th.join();
		s.close();
		
		verify(ffto, never()).executeAsMaster(any(), any());
        verify(hco, times(1)).executeAsMaster(any(), any(), any());
        verify(tmsm, times(1)).unlock(LockType.INBOUND);
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
	
}
