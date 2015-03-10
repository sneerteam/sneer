package sneer.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import sneer.android.gcm.GcmRegistrationAlarmReceiver;
import sneer.android.impl.SneerAndroidImpl;

public class SneerApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		//Do not delete this. We must revive the simulator for rapid UI development.
//		SneerAndroidSingleton.setInstance(isCoreAvailable()
//			? new SneerAndroidImpl(getApplicationContext())
//			: new SneerAndroidSimulator(getApplicationContext()));


		Context app = getApplicationContext();
		SneerAndroidSingleton.setInstance(new SneerAndroidImpl(app));
		Notifier.start(app);
		GcmRegistrationAlarmReceiver.schedule(app);
	}


	public void checkPlayServices(Activity activity) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				new AlertDialog.Builder(activity)
						.setTitle("Missing Google Play services")
						.setMessage("Some features such as using maps and receiving notifications when idle will not work without Google Play Services, which are missing from this phone.")
						.setPositiveButton("OK", null)
						.show();
			} else {
				Log.i(getClass().getName(), "This device is not supported.");
				activity.finish();
			}
		}
	}

}
