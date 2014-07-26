package sneer.android.main;

import android.content.*;
import android.util.*;

public class SneerBroadcastReceiver extends BroadcastReceiver {
	
	@Override
    public void onReceive(Context context, Intent intent) {
		Log.d(SneerBroadcastReceiver.class.getSimpleName(), "Starting Sneer background service");
        Intent startServiceIntent = new Intent(context, SneerAndroidService.class);
        context.startService(startServiceIntent);
    }
	
}
