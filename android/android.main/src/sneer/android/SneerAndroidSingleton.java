package sneer.android;

import android.content.Context;

import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.impl.SneerAndroidSimulator;
import sneer.commons.exceptions.Exceptions;

import static sneer.android.impl.SneerAndroidImpl.isCoreAvailable;

public class SneerAndroidSingleton {

	private static SneerAndroid INSTANCE;

    synchronized
    public static void ensureInstance(Context context) {
        if (INSTANCE != null)
            return;
        setInstance(isCoreAvailable()
                ? new SneerAndroidImpl(context)
                : new SneerAndroidSimulator(context));
    }

	private static void setInstance(SneerAndroid sneerAndroid) {
		Exceptions.check(sneerAndroid != null);
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
