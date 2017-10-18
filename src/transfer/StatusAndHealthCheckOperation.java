package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;

/**
 * Main goal:
 * Exists the following corner case:
 * 1. Mater communication thread in READY state
 * 2. Slave sends health check
 * 3. Slave receives status = READY
 * 4. Master communication thread changes its status to BUSY
 * 5. Slave sends get status request
 * In this moment master communication is BUSY state. In BUSY state it additionally to health check 
 * must handle status check request. Such case is possible only for master communication thread 
 * transition READY -> BUSY. 
 * 
 * For major part of requests(when no master communication thread transition READY -> BUSY) only 
 * health check operation is working 
 */
public class StatusAndHealthCheckOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseTransferOperations bto;
	
	private HealthCheckOperation hco;
	
	private StatusTransferOperation sto;

	public StatusAndHealthCheckOperation(BaseTransferOperations bto, HealthCheckOperation hco, StatusTransferOperation sto) {
		super();
		this.hco = hco;
		this.sto = sto;
		this.bto = bto;
	}
	
	public void executeAsMaster(OutputStream os, InputStream is, MasterStatus ms) throws IOException, WrongOperationException {
		//TODO: Refactor move pushbackInputStream higher to transfer manager
		PushbackInputStream pushbackInputStream = new PushbackInputStream(is);

		OperationType ot = bto.checkOperationType(pushbackInputStream);
		switch (ot) {
		case REQUEST_MASTER_STATUS_START: 
			sto.executeAsMaster(os, pushbackInputStream, ms);
			break;
		case REQUEST_HEALTHCHECK_START:
			hco.executeAsMaster(os, pushbackInputStream, ms);
			break;
		default:
			throw new WrongOperationException();
		}
	}
	
}
