package transfer;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO: Rename to Member state monitor 

/**
 * Main goal of this monitor is to restrict all possible activities to only one ACTIVE at a time.
 * This could be processing of inbound communication(download from current member), outbound 
 * communication (download from external member) or local repo scan. This is to ensure no repo 
 * corruption could happen because of unexpected effects of parallel execution.  
 */
public class TransferManagerStateMonitor {

	private Lock lock;
	
	private Lock releaseLock;
	
	private volatile LockType lockType;
	
	public TransferManagerStateMonitor() {
		this.lock = new ReentrantLock();
		this.releaseLock = new ReentrantLock();
		lockType = LockType.NONE;
	}
	
	/**
	 * Try to acquire lock. Returns result of this. In successful case when result is true
	 * lock is acquired 
	 * 
	 * @param type
	 * @return
	 */
	public boolean lock(LockType type) {
		if(lockType == LockType.NONE && lock.tryLock()) {
			lockType = type;
			return true;
		}
		return false;
	}

	/**
	 *  Try to release lock
	 *  
	 *  Only one gap that in theory is unreachable: different thread with the same type
	 *  invokes unlock. Leads to lock.unlock() fail(must be invoked form thread thta acquired lock).
	 *
	 *  Possible issues:
	 *  (1) double unlock invocation (second unlock might happen in catch block if unexpected
	 *	    exception is thrown).
	 *  (2) OutboundTransferManager thread acquires lock after inbound communication releases
	 *      it, however then inbound communication thread fails and in catch block releases
	 *      lock one more time. This time it's already acquired by OutboundTransferManager thread
	 *      that is currently in progress.
	 */
	public void unlock(LockType type) {
		if (releaseLock.tryLock()) {
			if(lockType == type) {
				lock.unlock();
				lockType = LockType.NONE;
			}
			releaseLock.unlock();
		}
	}
	
	public enum LockType {
		INBOUND,
		OUTBOUND,
		NONE
	}
	
}
