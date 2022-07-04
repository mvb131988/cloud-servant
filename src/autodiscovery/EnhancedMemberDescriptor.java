package autodiscovery;

public class EnhancedMemberDescriptor {

	private MemberDescriptor md;
	
	private int failureCounter;

	public EnhancedMemberDescriptor(MemberDescriptor md, int failureCounter) {
		super();
		this.md = md;
		this.failureCounter = failureCounter;
	}

	public MemberDescriptor getMd() {
		return md;
	}

	public void setMd(MemberDescriptor md) {
		this.md = md;
	}

	public int getFailureCounter() {
		return failureCounter;
	}

	public void setFailureCounter(int failureCounter) {
		this.failureCounter = failureCounter;
	}
	
}
