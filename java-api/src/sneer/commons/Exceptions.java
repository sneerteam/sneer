package sneer.commons;


public class Exceptions {

	public static String asNiceMessage(Exception e) {
		return e.getClass().getSimpleName() + ": " + e.getMessage();
	}

	
	public static void check(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

}
