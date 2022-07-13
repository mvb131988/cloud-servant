package transfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;

import transfer.TransferManagerStateMonitor.LockType;

public class TransferManagerStateMonitorTest {

	@Test
	public void test1() throws InterruptedException {
		TransferManagerStateMonitor tmsm = new TransferManagerStateMonitor();
		Work1 w1 = new Work1(tmsm);
		Thread th1 = new Thread(w1);
		th1.start();
		
		while (!w1.isLocked()) {
			Thread.sleep(10);
		}
		
		boolean res = tmsm.lock(LockType.INBOUND);
		assertFalse(res);
		
		w1.terminate();
		th1.join();
		
		res = tmsm.lock(LockType.INBOUND);
		assertTrue(res);
		tmsm.unlock(LockType.INBOUND);
	}
	
	@Test
	public void test2() throws InterruptedException {
		TransferManagerStateMonitor tmsm = new TransferManagerStateMonitor();
		Work1 w1 = new Work1(tmsm);
		Thread th1 = new Thread(w1);
		th1.start();
		
		while (!w1.isLocked()) {
			Thread.sleep(10);
		}
		
		tmsm.unlock(LockType.OUTBOUND);
		boolean res = tmsm.lock(LockType.OUTBOUND);
		assertFalse(res);
		
		w1.terminate();
		th1.join();
	}
	
	@Test
	public void test3() throws InterruptedException, 
							   IllegalArgumentException, 
							   IllegalAccessException, 
							   NoSuchFieldException, 
							   SecurityException 
	{
		TransferManagerStateMonitor tmsm = new TransferManagerStateMonitor();
		boolean res = tmsm.lock(LockType.OUTBOUND);
		assertTrue(res);
		res = tmsm.lock(LockType.OUTBOUND);
		assertFalse(res);
		
		tmsm.unlock(LockType.OUTBOUND);
		
		Lock mockLock = mock(Lock.class);
		setPrivateFieldValue(tmsm, "lock", mockLock);
		tmsm.unlock(LockType.OUTBOUND);
		verify(mockLock, times(0)).unlock();
	}
	
	private static class Work1 implements Runnable{

		private boolean terminate;
		
		private boolean locked;
		
		private TransferManagerStateMonitor tmsm;
		
		public Work1(TransferManagerStateMonitor tmsm) {
			this.terminate = false;
			this.locked = false;
			this.tmsm = tmsm;
		}

		@Override
		public void run() {
			while (!this.terminate) {
				if (!locked) {
					tmsm.lock(LockType.INBOUND);
					locked = true;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			tmsm.unlock(LockType.INBOUND);
		}
		
		public void terminate() {
			this.terminate = true;
		}

		public boolean isLocked() {
			return locked;
		}
	
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
