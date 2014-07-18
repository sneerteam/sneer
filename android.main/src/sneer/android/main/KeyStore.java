package sneer.android.main;

import java.io.*;

import android.content.*;
import android.content.SharedPreferences.Editor;
import android.preference.*;

public class KeyStore {

	private static final String PUBLIC_KEY = "public_key";

	private static byte[] publicKey;

	static public byte[] publicKey() {
		if (publicKey == null) throw new IllegalStateException("initKeys(context) must be called first");
		return publicKey;
	}
	
	public static void initKeys(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		long key = prefs.getLong(PUBLIC_KEY, 0L);
		if (key == 0L) {
			key = System.nanoTime() + System.currentTimeMillis();
			write(prefs, PUBLIC_KEY, key);
		}

		byte[] keyBytes = asBytes(key);
		publicKey = new byte[32];
		System.arraycopy(keyBytes, 0, publicKey, 0, keyBytes.length);
	}

	private static byte[] asBytes(long key) {
		try {
			return ("" + key).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException();
		}
	}

	private static void write(SharedPreferences prefs, String pref, long longValue) {
		Editor editor = prefs.edit();
		editor.putLong(pref, longValue);
		editor.commit();
	}
	

}
