package sneerteam.snapi;

import java.util.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.observables.ConnectableObservable;
import rx.schedulers.*;
import rx.subjects.ReplaySubject;
import android.content.Context;
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
	}

	private ReplaySubject<CloudConnection> eventualCloud;
	private Subscription subscription;

	public static Cloud cloudFor(final Context context) {
		return cloudFor(context, Schedulers.immediate());
	}
	
	public static Cloud cloudObservingOnCurrentThread(Context context) {
		return cloudFor(context, AndroidSchedulers.handlerThread(new Handler()));
	}

	public static Cloud cloudObservingOnAndroidMainThread(Context context) {
		return cloudFor(context, AndroidSchedulers.mainThread());
	}

	public static Cloud cloudFor(final Context context, Scheduler scheduler) {
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
