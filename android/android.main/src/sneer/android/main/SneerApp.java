package sneer.android.main;

import static sneer.android.impl.SneerAndroidImpl.isCoreAvailable;
import sneer.android.impl.SneerAndroidImpl;
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
