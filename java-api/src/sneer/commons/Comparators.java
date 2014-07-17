package sneer.commons;

public class Comparators {

	public static int compare(long x, long y) {
	    return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

}
