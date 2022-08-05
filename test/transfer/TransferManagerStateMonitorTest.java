package transfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;

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
		
		boolean res = tmsm.lock();
		assertFalse(res);
		
		w1.terminate();
		th1.join();
		
		res = tmsm.lock();
		assertTrue(res);
		tmsm.unlock();
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
		
		tmsm.unlock();
		boolean res = tmsm.lock();
		assertFalse(res);
		
		w1.terminate();
		th1.join();
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
					tmsm.lock();
					locked = true;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			tmsm.unlock();
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
