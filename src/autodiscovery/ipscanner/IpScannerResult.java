package autodiscovery.ipscanner;

public class IpScannerResult {

	private String ip;
	
	private String memberId;

	public IpScannerResult(String ip, String memberId) {
		super();
		this.ip = ip;
		this.memberId = memberId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	
}
