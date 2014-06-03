package sneerteam.snapi;

import java.util.*;
import java.util.concurrent.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.observables.*;
import rx.schedulers.*;
import rx.subjects.*;
import android.content.*;
import android.os.*;

public class Cloud {
	
	private final class CloudPathImpl implements CloudPath {

		private List<Object> segments;

		public CloudPathImpl(List<Object> segments) {
			this.segments = segments;
		}

		@Override
		public Observable<PathEvent> children() {
			return eventualCloud.flatMap(new Func1<CloudConnection, Observable<PathEvent>>() {@Override public Observable<PathEvent> call(CloudConnection cloud) {
				return cloud.path(segments).children();
			}});
		}

		@Override
		public CloudPath append(Object segment) {
			return path(Path.append(segments, segment));
		}

		@Override
		public void pub() {
			eventualCloud.first().subscribe(new Action1<CloudConnection>() {@Override public void call(CloudConnection cloud) {
				cloud.path(segments).pub();
			}});
		}

		@Override
		public void pub(final Object value) {
			eventualCloud.first().subscribe(new Action1<CloudConnection>() {@Override public void call(CloudConnection cloud) {
				cloud.path(segments).pub(value);
			}});
		}


		@Override
		public Observable<Object> value() {
			return eventualCloud.flatMap(new Func1<CloudConnection, Observable<Object>>() {@Override public Observable<Object> call(CloudConnection cloud) {
				return cloud.path(segments).value();
			}});
		}

        @Override
        public Observable<Boolean> exists(final long timeout, final TimeUnit unit) {
            return Observable.create(new OnSubscribe<Boolean>() {@Override public void call(final Subscriber<? super Boolean> subscriber) {
                eventualCloud.subscribe(new Action1<CloudConnection>() {@Override public void call(CloudConnection cloud) {
                    final Object token = new Object();
                    Observable.merge(cloud.path(segments).value(), Observable.from(token).delay(timeout, unit))
                    .first()
                    .observeOn(cloud.scheduler())
                    .subscribe(new Action1<Object>() {@Override public void call(Object value) {
                        subscriber.onNext(value != token);
                        subscriber.onCompleted();
                    }});
                }});
            }});
        }
	}

	private ReplaySubject<CloudConnection> eventualCloud;
	private Subscription subscription;

	public static Cloud cloudFor(Context context) {
		return onScheduler(context, Schedulers.immediate());
	}
	
	public static Cloud onCurrentThread(Context context) {
		return onScheduler(context, AndroidSchedulers.handlerThread(new Handler()));
	}

	public static Cloud onAndroidMainThread(Context context) {
		return onScheduler(context, AndroidSchedulers.mainThread());
	}

	public static Cloud onScheduler(final Context context, Scheduler scheduler) {
		return new Cloud(CloudServiceConnection.cloudFor(context, scheduler).publish());
	}
	

	public Cloud(ConnectableObservable<CloudConnection> eventualCloud) {
		this.eventualCloud = ReplaySubject.create();
		eventualCloud.subscribe(this.eventualCloud);
		subscription = eventualCloud.connect();
	}

	public void dispose() {
		subscription.unsubscribe();
	}

	public CloudPath path(Object... segments) {
		return path(Arrays.asList(segments));
	}

	public CloudPath path(List<Object> list) {
		return new CloudPathImpl(list);
	}
	
	public Observable<byte[]> ownPublicKey() {
		return Observable.create(new OnSubscribe<byte[]>() {@Override public void call(final Subscriber<? super byte[]> subscriber) {
			eventualCloud.subscribe(new Action1<CloudConnection>() {@Override public void call(CloudConnection cloud) {
				try {
					subscriber.onNext(cloud.ownPublicKey());
					subscriber.onCompleted();
				} catch (RemoteException e) {
					subscriber.onError(e);
				}
			}});
		}});
	}
}
