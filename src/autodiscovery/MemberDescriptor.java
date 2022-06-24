package autodiscovery;

public class MemberDescriptor {

	// each member presented in the cluster has a unique memberId
	private String memberId;
	
	private MemberType memberType;
	
	private String ipAddress;

	public MemberDescriptor(String memberId, MemberType memberType, String ipAddress) {
		super();
		this.memberId = memberId;
		this.memberType = memberType;
		this.ipAddress = ipAddress;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public MemberType getMemberType() {
		return memberType;
	}

	public void setMemberType(MemberType memberType) {
		this.memberType = memberType;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
}
