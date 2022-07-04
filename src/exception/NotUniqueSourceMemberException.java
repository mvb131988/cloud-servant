package exception;

/**
 * Is thrown if exist more than one SOURCE member with non null 
 * ip address.
 */
public class NotUniqueSourceMemberException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NotUniqueSourceMemberException() {
		super("Only one SOURCE member must exist in the datacenter");
	}
	
	public NotUniqueSourceMemberException(String message) {
		super(message);
	}

}
