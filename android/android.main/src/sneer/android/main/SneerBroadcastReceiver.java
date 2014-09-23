package sneer.android.main;

import sneer.android.main.ipc.TupleSpaceService;
import android.content.*;
import android.util.*;

public class SneerBroadcastReceiver extends BroadcastReceiver {
	
	@Override
    public void onReceive(Context context, Intent intent) {
		Log.d(SneerBroadcastReceiver.class.getSimpleName(), "Starting Sneer background service");
        TupleSpaceService.startTupleSpaceService(context);
    }
	
}
