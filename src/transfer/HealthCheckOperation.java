package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;

public class HealthCheckOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseTransferOperations bto;
	
	public HealthCheckOperation(BaseTransferOperations bto) {
		super();
		this.bto = bto;
	}
	
	public void executeAsMaster(OutputStream os, InputStream is, MasterStatus ms) throws IOException, WrongOperationException {
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_HEALTHCHECK_START) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_HEALTHCHECK_START + " Actual: " + ot);
		}
		
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested healthcheck");
		bto.sendOperationType(os, OperationType.REQUEST_HEALTHCHECK_END);
		
		bto.sendMasterStatus(os, ms);
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_HEALTHCHECK_START) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_HEALTHCHECK_START + " Actual: " + ot);
		}
		bto.sendOperationType(os, OperationType.RESPONSE_HEALTHCHECK_END);
	}
	
	public MasterStatus executeAsSlave(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		bto.sendOperationType(os, OperationType.REQUEST_HEALTHCHECK_START);
		logger.info("[" + this.getClass().getSimpleName() + "] request MASTER healthcheck");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_HEALTHCHECK_END) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_HEALTHCHECK_END + " Actual: " + ot);
		}
		
		MasterStatus ms = bto.receiveMasterStatus(is);
		logger.info("[" + this.getClass().getSimpleName() + "] MASTER status is : " + ms);
		
		bto.sendOperationType(os, OperationType.RESPONSE_HEALTHCHECK_START);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_HEALTHCHECK_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_HEALTHCHECK_END + " Actual: " + ot);
		}
		
		return ms;
	}
	
}
