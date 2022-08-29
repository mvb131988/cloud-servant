package transfer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import exception.OutboundMemberNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import transfer.constant.MemberStatus;
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
										   OutboundMemberNotReadyDuringBatchTransfer,
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
		when(ap.getTransferPort()).thenReturn(8888);
		when(hco.outbound(any(OutputStream.class), any(InputStream.class)))
			.thenReturn(new StatusTransferContext(MemberStatus.READY, null));
		
		ServerSocket ss = new ServerSocket(8888);
		OutboundTransferManager otm = new OutboundTransferManager(mim, hco, ffto, tmsm, ap);
		invokePrivateMethod(otm, "runInternally");
		ss.close();
		
		verify(ffto, times(1)).outbound(any(OutputStream.class), 
											  any(InputStream.class),
											  eq("member2"));
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
										   OutboundMemberNotReadyDuringBatchTransfer,
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
		when(ap.getTransferPort()).thenReturn(8888);
		when(hco.outbound(any(OutputStream.class), any(InputStream.class)))
			.thenReturn(new StatusTransferContext(MemberStatus.READY, null));
		
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
