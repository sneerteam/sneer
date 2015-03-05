package sneer.android;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import me.sneer.R;
import sneer.commons.Clock;
import sneer.commons.Threads;

import java.util.Date;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		String messageType = gcm.getMessageType(intent);
		Log.d(getClass().getName(), messageType);

		acquireWakeLock(context);
//		createNotificationIfNecessary(context);

		setResultCode(Activity.RESULT_OK);
	}

	private static void acquireWakeLock(Context context) {
		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		@SuppressWarnings("deprecation")
		final PowerManager.WakeLock hopeForTuples = powerManager.newWakeLock(
			    PowerManager.FULL_WAKE_LOCK, // For testing. Use PARTIAL_LOCK later.
				"SneerWakelockTag");
		hopeForTuples.acquire();

		new Thread() { @Override public void run() {
			Threads.sleepFor(30 * 1000);
			hopeForTuples.release();
		}}.start();
	}

	static int count = 1;

	private static void createNotificationIfNecessary(Context context) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("GCM Received")
				.setContentText("Count: " + count++ + " on " + new Date())
				.setWhen(Clock.now())
				.setAutoCancel(true)
//              .setContentIntent(pendIntent)
				.setOngoing(false);

		NotificationManager man = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		man.notify("gcm notification", 0, builder.build());
	}


}
