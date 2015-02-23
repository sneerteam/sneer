package sneer.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.Collection;
import java.util.Date;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.android.gcm.GcmRegistrationAlarmReceiver;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.ui.MainActivity;
import sneer.commons.Clock;

import static sneer.android.SneerAndroidSingleton.sneer;

public class SneerApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

	    //Do not delete this. We must revive the simulator for rapid UI development.
//		SneerAndroidSingleton.setInstance(isCoreAvailable()
//			? new SneerAndroidImpl(getApplicationContext())
//			: new SneerAndroidSimulator(getApplicationContext()));

        SneerAndroidSingleton.setInstance(new SneerAndroidImpl(getApplicationContext()));

        GcmRegistrationAlarmReceiver.schedule(this);

//		Notifier.resume();
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
