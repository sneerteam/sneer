package sneer.admin;

/** An Exception with a very user-friendly message. */
public class UniqueConstraintViolated extends Exception {

	public UniqueConstraintViolated(String message) {
		super(message);
	}

	public UniqueConstraintViolated(String message, Throwable cause) {
		super(message, cause);
	}

	private static final long serialVersionUID = 1L;

}
