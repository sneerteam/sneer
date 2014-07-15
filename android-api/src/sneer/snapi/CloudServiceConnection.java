package sneer.snapi;

import static sneer.snapi.SneerUtils.*;
import rx.*;
import rx.functions.*;
import rx.schedulers.*;
import rx.subscriptions.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class CloudServiceConnection {

    public static Observable<CloudConnection> cloudFor(final Context context, final Scheduler scheduler) {
        
        if (context instanceof Activity) {
            showSneerInstallationMessageIfNecessary((Activity) context);
        }
        
        return Observable.create(new Observable.OnSubscribe<CloudConnection>() {

            @Override
            public void call(final Subscriber<? super CloudConnection> subscriber) {

                Intent bindIntent = new Intent("sneer.intent.action.BIND_CLOUD_SERVICE");
                bindIntent.setClassName("sneer.android.main", "sneer.android.main.CloudService");

                final ServiceConnection serviceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        subscriber.onNext(new CloudConnection(binder, scheduler));
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

    public static Observable<CloudConnection> cloudFor(Context context) {
        return cloudFor(context, Schedulers.immediate());
    }

}
