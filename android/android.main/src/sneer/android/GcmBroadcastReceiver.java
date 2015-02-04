package sneer.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

        String messageType = gcm.getMessageType(intent);

        Log.d(getClass().getName(), messageType);

        setResultCode(Activity.RESULT_OK);
    }
}
