package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.MemberDescriptor;
import autodiscovery.MemberIpIterator;
import autodiscovery.MemberIpMonitor;
import exception.BatchFileTransferException;
import exception.MasterNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import transfer.constant.MasterStatus;

public class OutboundTransferManager implements Runnable {

	private Logger logger = LogManager.getRootLogger();
	
	private final int socketSoTimeout;
	
	private final int masterPort;
	
	private MemberIpMonitor mim;
	
	private HealthCheckOperation hco;
	
	private FullFileTransferOperation ffto;
	
	private TransferManagerStateMonitor tmsm;
	
	public OutboundTransferManager(MemberIpMonitor mim, 
								   HealthCheckOperation hco, 
								   FullFileTransferOperation ffto,
								   TransferManagerStateMonitor tmsm,
								   AppProperties ap) 
	{
		this.mim = mim;
		this.hco = hco;
		this.ffto = ffto;
		this.tmsm = tmsm;
		this.socketSoTimeout = ap.getSocketSoTimeout();
		this.masterPort = ap.getMasterPort();
	}

	@Override
	public void run() {
		for (;;) {
			runInternally();
		}
	}
	
	private void runInternally() {
		try {
			MemberIpIterator iterator = mim.iterator();
			while(iterator.hasNext()) {
				if(tmsm.lock()) {
					MemberDescriptor md = iterator.next();
					
					Socket connection = connect(md.getIpAddress(), masterPort);
					// if transfer manager state monitor allows connection
					transfer(connection.getOutputStream(), connection.getInputStream());
					connection.close();
					
					tmsm.unlock();
				}
				// TODO: random delay between 1 and 10 seconds
				Thread.sleep(1000);
			}
		} catch (Exception ex) {
			logger.error(ex);

			// TODO: on unlock need to check class that acquired the lock
			// need to introduce locker types: outbound, inbound, repository
			// requires to avoid unlock for different locker type(ex: inbound
			// locker could be unlocked by outbound if inbound acquired lock
			// just after outbound unlock in try block)
			tmsm.unlock();
		}
	}
	
	private Socket connect(String masterIp, int masterPort) 
			throws UnknownHostException, IOException 
	{
		Socket member = null;
		
		logger.info("[" + this.getClass().getSimpleName() + "] opening socket to " 
						+ masterIp + ":" + masterPort);
		member = new Socket(masterIp, masterPort);
		member.setSoTimeout(socketSoTimeout);
		logger.info("[" + this.getClass().getSimpleName() + "]  socket to " 
						+ masterIp + ":" + masterPort + " opened");
		
		return member;
	}
	
	private void transfer(OutputStream os, InputStream is) 
			throws InterruptedException, 
				   IOException, 
				   MasterNotReadyDuringBatchTransfer, 
				   WrongOperationException, 
				   BatchFileTransferException 
	{	
		// status READY is received only when external member acquires lock for
		// the given healthcheck request. This could only when transfermanagerstate monitor
		// is free on the external member
		// healthcheck returns MASTER status
		MasterStatus status = hco.executeAsSlave(os, is).getMasterStatus();
		if(status == MasterStatus.READY) {
			
			//TODO: still this is possible that between status check and transfer initiation
			//external member could change its status (to BUSY)
			ffto.executeAsSlave(os, is);
		}
	}
	
}
