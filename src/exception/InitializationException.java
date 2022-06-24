package exception;

/**
 * Is thrown when exception during initialization occurs 
 */
public class InitializationException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public InitializationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
