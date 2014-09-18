package sneer.android.main;

import static sneer.android.main.SneerAndroidCore.isCoreAvailable;
import sneer.android.main.ui.SneerAndroidProvider;
import android.app.Application;

public class SneerApp extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		SneerAndroid sneerAndroid;
		
		if (isCoreAvailable()) {
			sneerAndroid = new SneerAndroidCore(getApplicationContext());
		} else {
			sneerAndroid = new SneerAndroidSimulator(getApplicationContext());
		}
		
		SneerAndroidProvider.setInstance(sneerAndroid);
	}
	
}
