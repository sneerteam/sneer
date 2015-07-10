package sneer.android;

import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.commons.exceptions.Exceptions;

public class SneerAndroidSingletonOld { //TODO: This will be replaced by the SneerAndroidContainer.

	private static SneerAndroidOld INSTANCE;

	synchronized
	public static void setInstance(SneerAndroidOld sneerAndroid) {
		Exceptions.check(INSTANCE == null && sneerAndroid != null);
		SneerAndroidSingletonOld.INSTANCE = sneerAndroid;
	}


	public static SneerAndroidOld sneerAndroid() {
		return INSTANCE;
	}


	public static SneerAdmin admin() {
		return sneerAndroid().admin();
	}


	public static Sneer sneer() {
		return admin().sneer();
	}

}
