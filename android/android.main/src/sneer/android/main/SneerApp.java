package sneer.android.main;

import android.app.Application;

public class SneerApp extends Application {
	
	@Override
	public void onCreate() {

		SneerAndroid.init(getApplicationContext());

		
		super.onCreate();
	}
	
}
