package sneer.android.main.utils;

import android.util.Log;

public class LogUtils {

	public static void info(Class<?> c, String msg) {
		Log.i(c.getSimpleName(), msg);
	}
	
	public static void error(Class<?> c, String msg, Throwable t) {
		Log.e(c.getSimpleName(), msg, t);
	}

}
