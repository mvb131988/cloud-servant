package exception;

/**
 * Is thrown if the given member id (received from external member) is wrong
 * (no match found in local member members list) 
 */
public class WrongCloudMemberId extends Exception {

	private static final long serialVersionUID = 1L;
	
	public WrongCloudMemberId() {
		super("Wrong member id is provided. It's not found in local members list");
	}
	
	public WrongCloudMemberId(String message) {
		super(message);
	}
	
}
