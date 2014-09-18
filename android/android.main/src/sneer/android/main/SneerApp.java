package sneer.android.main;

import static sneer.android.main.SneerAndroidCore.*;
import sneer.android.main.ui.*;
import android.app.*;

public class SneerApp extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		SneerAndroid sneerAndroid;
		
		if (isCoreAvailable()) {
			sneerAndroid = new SneerAndroidCore(getApplicationContext());
		} else {
			sneerAndroid = new SneerAndroidSimulator();
		}
		
		SneerAndroidProvider.setInstance(sneerAndroid);
	}
	
}
