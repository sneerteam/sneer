package sneer.android.main;

import sneer.Sneer;
import sneer.admin.SneerAdmin;
import android.app.Activity;
import android.app.Application;
import android.content.Context;

public class SneerApp extends Application {
	
	private SneerAndroid sneerAndroid; 
	
	@Override
	public void onCreate() {
		super.onCreate();
		sneerAndroid = new SneerAndroidCore(getApplicationContext());
	}
	
	public static Sneer sneer(Context context) {
		return admin(context).sneer();
	}

	public static SneerAdmin admin(Context context) {
		return sneerAndroid(context).admin();
	}

	public static SneerAndroid sneerAndroid(Context context) {
		return ((SneerApp)((Activity)context).getApplication()).sneerAndroid();
	}

	public SneerAndroid sneerAndroid() {
		return sneerAndroid;
	}
}
