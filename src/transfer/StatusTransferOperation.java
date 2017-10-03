package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import transfer.constant.MasterStatus;
import transfer.constant.OperationType;

public class StatusTransferOperation {

	private BaseTransferOperations bto;
	
	public StatusTransferOperation(BaseTransferOperations bto) {
		super();
		this.bto = bto;
	}

	public void executeAsMaster(OutputStream os, InputStream is, MasterStatus ms) throws IOException{
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_MASTER_STATUS_START) {
			// error detected
		}
		bto.sendOperationType(os, OperationType.RESPONSE_MASTER_STATUS_START);
		
		bto.sendMasterStatus(os, ms);
		
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.REQUEST_MASTER_STATUS_END) {
			// error detected
		}
		bto.sendOperationType(os, OperationType.RESPONSE_MASTER_STATUS_END);
	}
	
	public MasterStatus executeAsSlave(OutputStream os, InputStream is) throws IOException{
		bto.sendOperationType(os, OperationType.REQUEST_MASTER_STATUS_START);
		OperationType ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_MASTER_STATUS_START) {
			// error detected
		}
		
		MasterStatus ms = bto.receiveMasterStatus(is);
		
		bto.sendOperationType(os, OperationType.REQUEST_MASTER_STATUS_END);
		ot = bto.receiveOperationType(is);
		if (ot != OperationType.RESPONSE_MASTER_STATUS_END) {
			// error detected
		}
		
		return ms;
	}
	
}
