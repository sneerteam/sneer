package sneer.android.main;

import android.app.Application;

public class SneerApp extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		SneerAndroid.init(getApplicationContext());
	}
	
}
