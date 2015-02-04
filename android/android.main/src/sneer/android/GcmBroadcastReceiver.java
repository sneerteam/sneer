package sneer.android;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import sneer.commons.Clock;

public class GcmBroadcastReceiver extends BroadcastReceiver {

    private static int next_id = (int)Clock.now();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        Log.d(getClass().getName(), messageType);

        createNotificationIfNecessary(context);

        setResultCode(Activity.RESULT_OK);
    }

    private void createNotificationIfNecessary(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("This is the Notification Title")
                .setContentText("This is the notification text. Bla bla bla")
                .setWhen(Clock.now())
                .setAutoCancel(true)
//              .setContentIntent(pendIntent)
                .setOngoing(false);

		NotificationManager man = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		man.notify("sneer notification", next_id++, builder.build());
    }


}
