package autodiscovery.ipscanner;

public class IpValidatorResult {

	private boolean result;
	
	private String memberId;

	public IpValidatorResult(boolean result, String memberId) {
		super();
		this.result = result;
		this.memberId = memberId;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	
}