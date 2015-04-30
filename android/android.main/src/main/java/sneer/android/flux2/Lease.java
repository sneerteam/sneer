package sneer.android.flux2;

import java.util.HashSet;
import java.util.Set;

import sneer.commons.Disposable;

public class Lease implements Disposable {

	Set<Object> held = new HashSet<>();

	@Override
	synchronized
	public Object dispose() {
		held = null;
		return null; //TODO Check if dispose really needs to return an Object.
	}

	synchronized
	public void hold(Object object) {
		if (held != null) held.add(object);
	}
}
