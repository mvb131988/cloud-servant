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
import exception.OutboundMemberNotReadyDuringBatchTransfer;
import exception.WrongOperationException;
import main.AppProperties;
import transfer.constant.MemberStatus;

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

	private Logger lockLogger = LogManager.getLogger("LockAcquiringLogger");

	private final int smallTimeout;
	
	private final int socketSoTimeout;
	
	private final int transferPort;
	
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
		this.transferPort = ap.getTransferPort();
		this.random = new Random();
		this.smallTimeout = ap.getSmallPoolingTimeout();
	}

	@Override
	public void run() {
		for (;;) {
			runInternally();
			
			try {
				Thread.sleep(smallTimeout);
			} catch(Exception ex) {
				//do nothing
			}
		}
	}
	
	private void runInternally() {
		MemberIpIterator iterator = mim.iterator();

		while (iterator.hasNext()) {
			MemberDescriptor md = iterator.next();

			try {
				if (tmsm.lock()) {

					lockLogger.info("Lock acquired for memberId=" + md.getMemberId());

					Socket connection = connect(md.getIpAddress(), transferPort);

					mim.resetFailureCounter(md);
					
					transfer(connection.getOutputStream(), 
							 connection.getInputStream(), 
							 md.getMemberId());
					connection.close();

					lockLogger.info("Lock releasing for memberId=" + md.getMemberId());
					
					tmsm.unlock();
					
					// random delay between 1 and smallTimeout in millis
					Thread.sleep(random.nextInt(10) * smallTimeout);
				}
			} catch (Exception ex) {
				logger.error("Exception: ", ex);

				lockLogger.info("Lock releasing due to exception happened");

				mim.incrementFailureCounter(md);

				tmsm.unlock();
			} catch (Throwable th) {
				logger.error("Throwable: ", th);
			}
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
	 * @throws OutboundMemberNotReadyDuringBatchTransfer
	 * @throws WrongOperationException
	 * @throws BatchFileTransferException
	 */
	private void transfer(OutputStream os, InputStream is, String memberId) 
			throws InterruptedException, 
				   IOException, 
				   OutboundMemberNotReadyDuringBatchTransfer, 
				   WrongOperationException, 
				   BatchFileTransferException 
	{	
		MemberStatus status = hco.outbound(os, is).getOutboundMemberStatus();
		
		logger.info("Neighbour member status: " + status);
		
		if(status == MemberStatus.READY) {			
			ffto.outbound(os, is, memberId);
		}
	}
	
}
