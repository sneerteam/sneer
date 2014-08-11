package sneer.commons.exceptions;

/** An Exception with a very user-friendly message. */
public class FriendlyException extends Exception {

	public FriendlyException(String veryUserFriendlyMessage) {
		super(veryUserFriendlyMessage);
	}

	private static final long serialVersionUID = 1L;

}
