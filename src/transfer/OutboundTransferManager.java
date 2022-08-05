package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

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

/**
 * Initiate connections to CLOUD or SOURCE members. It is run only on CLOUD members.
 * Process one connection at a time (given member perform file transfer only from one
 * member at a time). In order to connect to external member needs to acquire lock before
 * (no other operations, process of inbound connections or repo scan are allowed in parallel).
 * Once communication is finished (no matter external member is READY or BUSY) releases lock
 * and closes connection.
 */
public class OutboundTransferManager implements Runnable {

	private Logger logger = LogManager.getRootLogger();
	
	private final int socketSoTimeout;
	
	private final int masterPort;
	
	private MemberIpMonitor mim;
	
	private HealthCheckOperation hco;
	
	private FullFileTransferOperation ffto;
	
	private TransferManagerStateMonitor tmsm;
	
	private Random random;
	
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
		this.random = new Random();
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
				if (tmsm.lock()) {
				
					MemberDescriptor md = iterator.next();
					
					logger.info("Lock: " + md.getMemberId());
					
					Socket connection = connect(md.getIpAddress(), masterPort);
					transfer(connection.getOutputStream(), connection.getInputStream());
					connection.close();
					
					tmsm.unlock();
					
					logger.info("Unlock: " + md.getMemberId());
				}
				
				// TODO: random delay between 1 and 10 seconds
				Thread.sleep(random.nextInt(10) * 100);
			}
		} catch (Exception ex) {
			logger.error(ex);

			tmsm.unlock();
		} catch(Throwable th) {
			logger.error("Throwable", th);
		}
	}
	
	/**
	 * Try to establish connection with external member
	 * 
	 * @param memberIp
	 * @param transferPort
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private Socket connect(String memberIp, int transferPort) 
			throws UnknownHostException, IOException 
	{
		Socket member = null;
		
		logger.info("Opening socket to " + memberIp + ":" + transferPort);
		member = new Socket(memberIp, transferPort);
		member.setSoTimeout(socketSoTimeout);
		logger.info("Socket to " + memberIp + ":" + transferPort + " opened");
		
		return member;
	}
	
	/**
	 * Receives external member status. If external member is READY for files transmission
	 * with the given(local) member it sends READY status. Then files transmission happens.
	 * If external member is busy with the other task it sends BUSY status. In this case
	 * connection is closed.
	 * 
	 * @param os
	 * @param is
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws MasterNotReadyDuringBatchTransfer
	 * @throws WrongOperationException
	 * @throws BatchFileTransferException
	 */
	private void transfer(OutputStream os, InputStream is) 
			throws InterruptedException, 
				   IOException, 
				   MasterNotReadyDuringBatchTransfer, 
				   WrongOperationException, 
				   BatchFileTransferException 
	{	
		// status READY is received only when external member(inbound communication) acquires
		// lock for the given healthcheck request. This could only when transfermanagerstate
		// monitor is free on the external member healthcheck returns MASTER status
		MasterStatus status = hco.executeAsSlave(os, is).getMasterStatus();
		
		logger.info("Neighbour member status: " + status);
		
		if(status == MasterStatus.READY) {			
			ffto.executeAsSlave(os, is);
		}
	}
	
}
