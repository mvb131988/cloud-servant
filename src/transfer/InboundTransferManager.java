package transfer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import main.AppProperties;
import transfer.constant.MasterStatus;

/**
 * Accept connections from CLOUD members. It is run on both SOURCE and CLOUD members.
 * When connection is accepted creates separate Communication thread for it. If the 
 * given member is READY (there are no active inbound, outbound communications and
 * no repo scan) then communication acquires lock on transfer manager state monitor 
 * and performs communication cycle. It then performs communication cycle (repo state
 * and files transfer happens). If lock couldn't be acquired then BUSY state is sent 
 * to the external node. In both cases communication is closed at the end and lock is
 * released.   
 */
public class InboundTransferManager implements Runnable {
	
	private Logger logger = LogManager.getRootLogger();
	
	private final int socketSoTimeout;
	
	private HealthCheckOperation hco;
	
	private FullFileTransferOperation ffto;
	
	private TransferManagerStateMonitor tmsm;

	private ServerSocket server;
	
	private boolean inTesting;
	
	public InboundTransferManager(HealthCheckOperation hco, 
								  FullFileTransferOperation ffto,
								  TransferManagerStateMonitor tmsm,
								  AppProperties ap) 
	{
		this.hco = hco;
		this.ffto = ffto;
		this.tmsm = tmsm;
		this.socketSoTimeout = ap.getSocketSoTimeout();
		this.inTesting = false;
		
		try {
			this.server = new ServerSocket(ap.getMasterPort());
		} catch (IOException e) {
			logger.error("[" + this.getClass().getSimpleName() + "] initialization fail", e);
		}
	}

	
	@Override
	public void run() {
		for(;;) {
			try {
				Socket in = acceptInbound();
				Thread th = new Thread(new Communication(in, tmsm, hco, ffto));
				th.setName("Communication thread");
				th.start();
				
				if(inTesting) {
					th.join();
					break;
				}
			} catch(Exception ex) {
				logger.error(ex);
			}
		}
	}
	
	/**
	 * Accepts connection from the external member (inbound connection)
	 * @throws IOException 
	 */
	private Socket acceptInbound() throws IOException {
		Socket in;
		logger.info("[" + this.getClass().getSimpleName() + "] waiting for slave to connect");
		in = server.accept();
		in.setSoTimeout(socketSoTimeout);
		logger.info("[" + this.getClass().getSimpleName() + "] slave connected");
		return in;
	}
	
	/**
	 * Handle single communication between local member and external member, defined by
	 * inbound connection.   
	 */
	private static class Communication implements Runnable {

		private Logger logger = LogManager.getRootLogger();
		
		private Logger orderLogger = LogManager.getLogger("execution-order-logger");
		
		private Socket in;
		
		private TransferManagerStateMonitor tmsm;
		
		private HealthCheckOperation hco;
		
		private FullFileTransferOperation ffto;
		
		public Communication(Socket in, 
							 TransferManagerStateMonitor tmsm,
							 HealthCheckOperation hco,
							 FullFileTransferOperation ffto) 
		{
			this.in = in;
			this.tmsm = tmsm;
			this.hco = hco;
			this.ffto = ffto;
		}
		
		@Override
		public void run() {
			try {
				if (tmsm.lock()) {
					
					orderLogger.info("InboundTransferManager acquires lock");
					
					transfer(in.getOutputStream(), 
							 new PushbackInputStream(in.getInputStream()));
				} else {
					transferBusy(in.getOutputStream(), 
								 new PushbackInputStream(in.getInputStream()));
				}
			} catch (IOException | WrongOperationException ex) {
				logger.error("Communication exception ", ex);
			} finally {
				try {
					in.close();
				} catch (IOException ex) {
					logger.error("Fatal error trying to close socket ", ex);
				}
				tmsm.unlock();
			}
		}

		private void transfer(OutputStream os, PushbackInputStream pushbackInputStream)
				throws IOException, WrongOperationException {
			hco.executeAsMaster(os, pushbackInputStream, MasterStatus.READY);
			ffto.executeAsMaster(os, pushbackInputStream);
		}

		private void transferBusy(OutputStream os, PushbackInputStream pushbackInputStream)
				throws IOException, WrongOperationException {
			hco.executeAsMaster(os, pushbackInputStream, MasterStatus.BUSY);
		}
	}
		
}
