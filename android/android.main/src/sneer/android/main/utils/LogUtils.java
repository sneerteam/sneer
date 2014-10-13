package sneer.android.main.utils;

import sneer.android.main.ipc.SingleMessageSession;
import android.util.Log;

public class LogUtils {

	public static void info(String string) {
		Log.i(SingleMessageSession.class.getSimpleName(), string);
	}

}
