package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;

public class StatusTransferOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseTransferOperations bto;
	
	public StatusTransferOperation(BaseTransferOperations bto) {
		super();
		this.bto = bto;
	}

	public void executeAsMaster(OutputStream os, InputStream is, MasterStatus ms) throws IOException, WrongOperationException{
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_MASTER_STATUS_START) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_MASTER_STATUS_START + " Actual: " + ot);
		}
		
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested status");
		bto.sendOperationType(os, OperationType.RESPONSE_MASTER_STATUS_START);
		
		bto.sendMasterStatus(os, ms);
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_MASTER_STATUS_END) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_MASTER_STATUS_END + " Actual: " + ot);
		}
		bto.sendOperationType(os, OperationType.RESPONSE_MASTER_STATUS_END);
	}
	
	public MasterStatus executeAsSlave(OutputStream os, InputStream is) throws IOException, WrongOperationException{
		bto.sendOperationType(os, OperationType.REQUEST_MASTER_STATUS_START);
		logger.info("[" + this.getClass().getSimpleName() + "] request MASTER status");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_MASTER_STATUS_START) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_MASTER_STATUS_START + " Actual: " + ot);
		}
		
		MasterStatus ms = bto.receiveMasterStatus(is);
		logger.info("[" + this.getClass().getSimpleName() + "] MASTER status is : " + ms);
		
		bto.sendOperationType(os, OperationType.REQUEST_MASTER_STATUS_END);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_MASTER_STATUS_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_MASTER_STATUS_END + " Actual: " + ot);
		}
		
		return ms;
	}
	
}
