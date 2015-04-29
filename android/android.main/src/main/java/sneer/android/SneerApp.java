package sneer.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import sneer.android.gcm.GcmRegistrationAlarmReceiver;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.ipc.PartnerSessions;

public class SneerApp extends Application {

	private static final boolean DEVELOPER_MODE = false;

	@Override
	public void onCreate() {
		super.onCreate();

		if (DEVELOPER_MODE) {
			StrictMode.setThreadPolicy(
					new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(
					new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
		}

		//Do not delete this. We must revive the simulator for rapid UI development.
//		SneerAndroidSingleton.setInstance(isCoreAvailable()
//			? new SneerAndroidImpl(getApplicationContext())
//			: new SneerAndroidSimulator(getApplicationContext()));


		Context app = getApplicationContext();
		SneerAndroidSingleton.setInstance(new SneerAndroidImpl(app));
		PartnerSessions.init(SneerAndroidSingleton.sneer().conversations());
		Notifier.start(app);
		GcmRegistrationAlarmReceiver.schedule(app);
	}


	public void checkPlayServices(Activity activity) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode == ConnectionResult.SUCCESS) return;
		if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
			new AlertDialog.Builder(activity)
					.setTitle("Missing Google Play Services")
					.setMessage("Some features such as using maps and receiving notifications when idle will not work without Google Play Services, which are missing from this phone.")
					.setPositiveButton("OK", null)
					.show();
		} else {
			String msg = "This device is not supported.";
			Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
			Log.i(getClass().getName(), msg);
			activity.finish();
		}
	}

}
