package sneer.android.main.utils;

import android.util.Log;

public class LogUtils {

	public static void info(Class<?> class1, String msg) {
		Log.i(class1.getSimpleName(), msg);
	}
	
	public static void error(Class<?> class1, String msg, Throwable t) {
		Log.e(class1.getSimpleName(), msg, t);
	}

	public static void warn(Class<?> class1, String msg, Exception e) {
		Log.w(class1.getSimpleName(), "Error loading bitmap", e);
	}
	
	public static void debug(Class<?> class1, String msg) {
		Log.d(class1.getSimpleName(), msg);
	}

}
