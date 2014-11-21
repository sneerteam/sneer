package sneer.android;

import sneer.android.ipc.TupleSpaceService;
import sneer.android.utils.LogUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SneerBoot extends BroadcastReceiver {

	@Override
    public void onReceive(Context context, Intent intent) {
		LogUtils.debug(SneerBoot.class, "Starting Sneer background service");
        TupleSpaceService.startTupleSpaceService(context);
    }

}
