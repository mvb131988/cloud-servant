package protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import protocol.MasterTransferManager.MasterSlaveCommunicationThread;

/**
 * Synchronizer class which main purpose is to provide separate access to
 * MasterSlaveCommunicationThread objects. It will provide all objects of the
 * pool for resume/pause/status operations, which are already stored at the
 * moment of invocation(get). However if a new connection is accepted and a new
 * communication is saved in the pool(put) just after the get returns, it is not
 * going to participate into resume/pause/status(at moment of saving and after
 * these operations may be in progress). It will participate in these operations
 * only on the next get method invocation. Until that moment it will have
 * default behavior, that was set on its creation.
 */
public class MasterSlaveCommunicationPool {

	private List<MasterTransferManager.MasterSlaveCommunicationThread> pool;
	
	private Lock lock;
	
	public MasterSlaveCommunicationPool() {
		this.pool = new ArrayList<>();
		this.lock = new ReentrantLock();
	}

	public List<MasterTransferManager.MasterSlaveCommunicationThread> get(){
		lock.lock();
		ArrayList<MasterSlaveCommunicationThread> arrayList = new ArrayList<>(pool);		
		lock.unlock();
		return arrayList;
	}
	
	public void add(MasterTransferManager.MasterSlaveCommunicationThread communication){
		lock.lock();
		pool.add(communication);
		lock.unlock();
	}
	
	public void remove(MasterTransferManager.MasterSlaveCommunicationThread communication){
		lock.lock();
		pool.removeIf((MasterTransferManager.MasterSlaveCommunicationThread c) -> {
			return c == communication;
		});
		lock.unlock();
	}
	
}
