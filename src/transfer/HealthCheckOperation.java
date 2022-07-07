package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.ipscanner.MemberIdFinder;
import exception.WrongOperationException;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;
import transfer.context.StatusTransferContext;

public class HealthCheckOperation implements MemberIdFinder {

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
	
	public StatusTransferContext executeAsSlave(OutputStream os, InputStream is) throws IOException, WrongOperationException {
		bto.sendOperationType(os, OperationType.REQUEST_HEALTHCHECK_START);
		logger.info("[" + this.getClass().getSimpleName() + "] request MASTER healthcheck");
		
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_HEALTHCHECK_END) {
			throw new WrongOperationException("Expected: " + OperationType.REQUEST_HEALTHCHECK_END + " Actual: " + ot);
		}
		
		StatusTransferContext stc = bto.receiveMasterStatus(is);
		logger.info("[" + this.getClass().getSimpleName() + "] MASTER status is : " + stc.getMasterStatus());
		
		bto.sendOperationType(os, OperationType.RESPONSE_HEALTHCHECK_START);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_HEALTHCHECK_END) {
			throw new WrongOperationException("Expected: " + OperationType.RESPONSE_HEALTHCHECK_END + " Actual: " + ot);
		}
		
		return stc;
	}

	@Override
	public String memberId(OutputStream os, InputStream is) {
		StatusTransferContext stc = null;
		try {
			stc = executeAsSlave(os, is);
		} catch (IOException | WrongOperationException e) {
			logger.error("Exception during healthcheck operation occured: ", e);
		}
		return stc != null ? stc.getMemberId() : null;
	}
	
}
