package transfer.context;

import transfer.constant.MemberStatus;

public class StatusTransferContext {

	private MemberStatus outboundMemberStatus;
	
	private String memberId;

	public StatusTransferContext(MemberStatus memberStatus, String memberId) {
		this.outboundMemberStatus = memberStatus;
		this.memberId = memberId;
	}
	
	public MemberStatus getOutboundMemberStatus() {
		return outboundMemberStatus;
	}

	public String getMemberId() {
		return memberId;
	}
	
}
