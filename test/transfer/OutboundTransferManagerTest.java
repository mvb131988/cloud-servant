package transfer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import autodiscovery.MemberDescriptor;
import autodiscovery.MemberIpIterator;
import autodiscovery.MemberIpMonitor;
import autodiscovery.MemberType;
import exception.BatchFileTransferException;
import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import transfer.constant.MasterStatus;
import transfer.context.StatusTransferContext;

public class OutboundTransferManagerTest {

	@Test
	public void testRunInternally1() throws IllegalArgumentException, 
										   IllegalAccessException, 
										   NoSuchFieldException, 
										   SecurityException, 
										   NoSuchMethodException, 
										   InvocationTargetException, 
										   IOException, 
										   WrongOperationException, 
										   InterruptedException, 
										   MasterNotReadyDuringBatchTransfer,
										   BatchFileTransferException 
	{
		MemberIpMonitor mim = mock(MemberIpMonitor.class);
		HealthCheckOperation hco = mock(HealthCheckOperation.class);
		FullFileTransferOperation ffto = mock(FullFileTransferOperation.class);
		TransferManagerStateMonitor tmsm = mock(TransferManagerStateMonitor.class);
		AppProperties ap = mock(AppProperties.class);
		
		when(mim.iterator()).thenReturn(new MemberIpIterator() {

			private List<MemberDescriptor> mds = new ArrayList<>();
			int pos = 0;
			
			{
				mds.add(new MemberDescriptor("member2", MemberType.SOURCE, "localhost"));
			}
			
			@Override
			public MemberDescriptor next() {
				return mds.get(pos++);
			}

			@Override
			public boolean hasNext() {
				return pos != mds.size();
			}
			
		});
		
		when(tmsm.lock()).thenReturn(true);
		when(ap.getSocketSoTimeout()).thenReturn(1000);
		when(ap.getMasterPort()).thenReturn(8888);
		when(hco.executeAsSlave(any(OutputStream.class), any(InputStream.class)))
			.thenReturn(new StatusTransferContext(MasterStatus.READY, null));
		
		ServerSocket ss = new ServerSocket(8888);
		OutboundTransferManager otm = new OutboundTransferManager(mim, hco, ffto, tmsm, ap);
		invokePrivateMethod(otm, "runInternally");
		ss.close();
		
		verify(ffto, times(1)).executeAsSlave(any(OutputStream.class), any(InputStream.class));
		verify(tmsm, times(1)).unlock();
	}
	
	@Test
	public void testRunInternally2() throws IllegalArgumentException, 
										   IllegalAccessException, 
										   NoSuchFieldException, 
										   SecurityException, 
										   NoSuchMethodException, 
										   InvocationTargetException, 
										   IOException, 
										   WrongOperationException, 
										   InterruptedException, 
										   MasterNotReadyDuringBatchTransfer,
										   BatchFileTransferException 
	{
		MemberIpMonitor mim = mock(MemberIpMonitor.class);
		HealthCheckOperation hco = mock(HealthCheckOperation.class);
		FullFileTransferOperation ffto = mock(FullFileTransferOperation.class);
		TransferManagerStateMonitor tmsm = mock(TransferManagerStateMonitor.class);
		AppProperties ap = mock(AppProperties.class);
		
		when(mim.iterator()).thenReturn(new MemberIpIterator() {

			private List<MemberDescriptor> mds = new ArrayList<>();
			int pos = 0;
			
			{
				mds.add(new MemberDescriptor("member2", MemberType.SOURCE, "localhost"));
			}
			
			@Override
			public MemberDescriptor next() {
				return mds.get(pos++);
			}

			@Override
			public boolean hasNext() {
				return pos != mds.size();
			}
			
		});
		
		when(tmsm.lock()).thenReturn(true);
		when(ap.getSocketSoTimeout()).thenReturn(1000);
		when(ap.getMasterPort()).thenReturn(8888);
		when(hco.executeAsSlave(any(OutputStream.class), any(InputStream.class)))
			.thenReturn(new StatusTransferContext(MasterStatus.READY, null));
		
		OutboundTransferManager otm = new OutboundTransferManager(mim, hco, ffto, tmsm, ap);
		invokePrivateMethod(otm, "runInternally");
		verify(tmsm, times(1)).unlock();
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
	
}
