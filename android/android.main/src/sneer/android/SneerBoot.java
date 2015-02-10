package sneer.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import sneer.android.ipc.TupleSpaceService;
import sneer.android.utils.LogUtils;

import java.util.Calendar;

public class SneerBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
		LogUtils.debug(SneerBoot.class, "Starting Sneer background service");
        TupleSpaceService.startTupleSpaceService(context);

        // This is an attempt to make the CPU and network wake up periodically to fetch new tuples.
        // It can be used if Google Cloud Messaging (GCM) doesn't work.
//      startWatchDogTimer(context, intent);
    }

    public static final String TIMER_TAG = "TIMER_TAG";
    public static final int SECONDS_TIMEOUT = 30;
    private void startWatchDogTimer(Context context, Intent intent) {
        if (intent.getBooleanExtra(TIMER_TAG, false))
            return;

        Intent newIntent = new Intent(context, getClass());
        newIntent.putExtra(TIMER_TAG, true);

        PendingIntent pending = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, SECONDS_TIMEOUT);

        AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // InexactRepeating allows Android to optimize the energy consumption
        service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 1000 * SECONDS_TIMEOUT, pending);
    }

}
