package sneer.android.main;

import android.content.*;
import android.util.*;

public class CloudBroadcastReceiver extends BroadcastReceiver {
	
	@Override
    public void onReceive(Context context, Intent intent) {
		Log.d(CloudBroadcastReceiver.class.getSimpleName(), "Starting Sneer background service");
        Intent startServiceIntent = new Intent(context, CloudService.class);
        context.startService(startServiceIntent);
    }
	
}
