package sneer;

import sneer.refimpl.*;

public class SneerFactory {

	public static Sneer newSneer() {
		return new InMemoryLocalSneer();
	}


}
