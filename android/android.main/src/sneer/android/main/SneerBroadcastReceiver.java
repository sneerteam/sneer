package sneer.android.main;

import sneer.android.main.ipc.TupleSpaceService;
import sneer.android.main.utils.LogUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SneerBroadcastReceiver extends BroadcastReceiver {
	
	@Override
    public void onReceive(Context context, Intent intent) {
		LogUtils.debug(SneerBroadcastReceiver.class, "Starting Sneer background service");
        TupleSpaceService.startTupleSpaceService(context);
    }
	
}
