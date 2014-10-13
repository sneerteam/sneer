package sneer.android.main.utils;

import sneer.android.main.SneerAndroidCore;
import android.util.Log;

public class LogUtils {

	public static void info(Class<?> class1, String msg) {
		Log.i(class1.getSimpleName(), msg);
	}
	
	public static void error(Class<?> class1, String msg, Throwable t) {
		Log.e(class1.getSimpleName(), msg, t);
	}

	public static void w(Class<SneerAndroidCore> class1, String msg, Exception e) {
		Log.w(class1.getSimpleName(), "Error loading bitmap", e);
	}

}
