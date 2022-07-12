package transfer;

import transfer.constant.MasterStatus;

// Member state monitor 
public class TransferManagerStateMonitor {

	//TODO: Rename master status
	public synchronized MasterStatus getState() {
		return null;
	}
	
	// Try to change status from READY to BUSY
	public synchronized boolean lock() {
		//use tryLock here
		return false;
	}

	// Try to change status from BUSY to READY
	public synchronized boolean unlock() {return false;}
	
}
