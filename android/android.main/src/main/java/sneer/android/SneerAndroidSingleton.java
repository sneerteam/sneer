package sneer.android;

import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.commons.exceptions.Exceptions;

public class SneerAndroidSingleton {

	private static SneerAndroid INSTANCE;

	synchronized
	public static void setInstance(SneerAndroid sneerAndroid) {
		Exceptions.check(INSTANCE == null && sneerAndroid != null);
		SneerAndroidSingleton.INSTANCE = sneerAndroid;
	}


	public static SneerAndroid sneerAndroid() {
		return INSTANCE;
	}


	public static SneerAdmin admin() {
		return sneerAndroid().admin();
	}


	public static Sneer sneer() {
		return admin().sneer();
	}

}
