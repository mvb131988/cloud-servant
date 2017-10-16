package exception;

/**
 * Is thrown if operation type is not as expected
 */
public class WrongOperationException extends Exception {

	private static final long serialVersionUID = 1L;

	public WrongOperationException() {
		
	}
	
	public WrongOperationException(String message) {
		super(message);
	}
	
}
