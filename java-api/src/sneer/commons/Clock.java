package sneer.commons;

import static sneer.commons.exceptions.Exceptions.check;

public class Clock {

	private static volatile long now = -1;
	

	public static void mock() {
		check(!isMocked());
		now = 0;
	}

	
	public static void advance(long millis) {
		check(millis > 0);
		check(isMocked());
		now += millis;
	}

	
	public static void tick() {
		advance(1);
	}

	
	public static long now() {
		return isMocked() ? now : System.currentTimeMillis();
	}


	private static boolean isMocked() {
		return now != -1;
	}
}
