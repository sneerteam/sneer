package sneer.android;

import static sneer.android.impl.SneerAndroidImpl.isCoreAvailable;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.impl.SneerAndroidSimulator;
import android.app.Application;

public class SneerApp extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		SneerAndroidSingleton.setInstance(isCoreAvailable()
			? new SneerAndroidImpl(getApplicationContext())
			: new SneerAndroidSimulator(getApplicationContext()));
	}
	
}
