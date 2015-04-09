package sneer.android.ipcold;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public class TupleSpaceService extends Service {

    private Subscription subscription;

    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		logInterval();
		return START_STICKY;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (subscription != null)
            subscription.unsubscribe();
    }

    @Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public static void startTupleSpaceService(Context context) {
		Intent startServiceIntent = new Intent(context, TupleSpaceService.class);
	    context.startService(startServiceIntent);
	}

    private void logInterval() {
        if (subscription != null)
            return;
        subscription = Observable.interval(5, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long interval) {
            return "IT'S ALIVE: " + System.currentTimeMillis() / 1000;
        }}).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
            Log.d("", obj.toString());
        }});
    }

}
