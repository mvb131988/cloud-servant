package exception;

public class WrongMemberId extends Exception {

	private static final long serialVersionUID = 1L;
	
	public WrongMemberId() {
		super("Wrong member id is provided. It's not found in local members list");
	}
	
	public WrongMemberId(String message) {
		super(message);
	}
	
}
