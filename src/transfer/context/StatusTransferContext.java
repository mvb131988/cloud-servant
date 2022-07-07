package transfer.context;

import transfer.constant.MasterStatus;

public class StatusTransferContext {

	private MasterStatus masterStatus;
	
	private String memberId;

	public StatusTransferContext(MasterStatus masterStatus, String memberId) {
		this.masterStatus = masterStatus;
		this.memberId = memberId;
	}
	
	public MasterStatus getMasterStatus() {
		return masterStatus;
	}

	public String getMemberId() {
		return memberId;
	}
	
}
