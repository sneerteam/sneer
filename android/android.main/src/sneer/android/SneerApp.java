package sneer.android;

import android.app.Application;

public class SneerApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
        SneerAndroidSingleton.ensureInstance(getApplicationContext());
    }

}
