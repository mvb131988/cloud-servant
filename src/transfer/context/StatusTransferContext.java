package transfer.context;

import transfer.constant.MemberStatus;

public class StatusTransferContext {

	private MemberStatus outboundMemberStatus;
	
	private String memberId;

	public StatusTransferContext(MemberStatus masterStatus, String memberId) {
		this.outboundMemberStatus = masterStatus;
		this.memberId = memberId;
	}
	
	public MemberStatus getOutboundMemberStatus() {
		return outboundMemberStatus;
	}

	public String getMemberId() {
		return memberId;
	}
	
}
