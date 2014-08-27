package sneer.commons.exceptions;

/** An Exception with a very user-friendly message. */
public class FriendlyException extends Exception {

	public FriendlyException(String veryUserFriendlyMessage) {
		super(veryUserFriendlyMessage);
	}

	public FriendlyException(String veryUserFriendlyMessage, Throwable cause) {
		super(veryUserFriendlyMessage, cause);
	}

	private static final long serialVersionUID = 1L;

}
