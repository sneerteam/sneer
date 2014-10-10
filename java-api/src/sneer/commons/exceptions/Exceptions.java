package sneer.commons.exceptions;

public class Exceptions {

	public static String asNiceMessage(Throwable t) {
		return t.getClass().getSimpleName() + ": " + t.getMessage();
	}

	
	public static void check(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

}
