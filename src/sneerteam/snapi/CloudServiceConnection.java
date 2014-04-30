package sneerteam.snapi;

import rx.*;
import rx.functions.*;
import rx.subscriptions.*;
import android.content.*;
import android.os.*;

public class CloudServiceConnection {
	
	public static Observable<CloudConnection> cloudFor(final Context context) {
		return Observable.create(new Observable.OnSubscribe<CloudConnection>() {

			@Override
			public void call(final Subscriber<? super CloudConnection> subscriber) {
				
				Intent bindIntent = new Intent("sneerteam.intent.action.BIND_CLOUD_SERVICE");
				bindIntent.setClassName("sneerteam.android.main", "sneerteam.android.main.CloudService");

				final ServiceConnection serviceConnection = new ServiceConnection() {
					@Override
					public void onServiceConnected(ComponentName name, IBinder binder) {
						subscriber.onNext(new CloudConnection(binder));
					}

					@Override
					public void onServiceDisconnected(ComponentName arg0) {
						subscriber.onCompleted();
					}
				};
				
				context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
				
				subscriber.add(Subscriptions.create(new Action0() {
					@Override
					public void call() {
						context.unbindService(serviceConnection);
					}
				}));
			}
		});

	}
	
}
