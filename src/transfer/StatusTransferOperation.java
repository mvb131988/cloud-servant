package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.WrongOperationException;
import transfer.constant.MemberStatus;
import transfer.constant.OperationType;
import transfer.context.StatusTransferContext;

// There is no need to check outbound member status during file transfer because it is 
// always READY until file transfer is not finished or exception occured
@Deprecated
public class StatusTransferOperation {

	private Logger logger = LogManager.getRootLogger();
	
	private BaseTransferOperations bto;
	
	public StatusTransferOperation(BaseTransferOperations bto) {
		super();
		this.bto = bto;
	}

	public void executeAsMaster(OutputStream os, InputStream is, MemberStatus ms) throws IOException, WrongOperationException{
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_MASTER_STATUS_START) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_MASTER_STATUS_START + " Actual: " + ot);
		}
		
		logger.info("[" + this.getClass().getSimpleName() + "] slave requested status");
		bto.sendOperationType(os, OperationType.RESPONSE_MASTER_STATUS_START);
		
		bto.sendMemberStatus(os, ms);
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_MASTER_STATUS_END) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_MASTER_STATUS_END + " Actual: " + ot);
		}
		bto.sendOperationType(os, OperationType.RESPONSE_MASTER_STATUS_END);
	}
	
	public StatusTransferContext executeAsSlave(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		bto.sendOperationType(os, OperationType.REQUEST_MASTER_STATUS_START);
		logger.info("[" + this.getClass().getSimpleName() + "] request MASTER status");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_MASTER_STATUS_START) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_MASTER_STATUS_START + " Actual: " + ot);
		}
		
		StatusTransferContext stc = bto.receiveOutboundMemberStatus(is);
		logger.info("[" + this.getClass().getSimpleName() + "] MASTER status is : " + stc.getOutboundMemberStatus());
		
		bto.sendOperationType(os, OperationType.REQUEST_MASTER_STATUS_END);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_MASTER_STATUS_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_MASTER_STATUS_END + " Actual: " + ot);
		}
		
		return stc;
	}
	
}
