package sneerteam.snapi;

import java.util.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.functions.*;
import rx.subscriptions.*;
import android.content.*;

public class Cloud {
	
	private final class EventualPathImpl implements EventualPath {

		private List<Object> segments;
		private CompositeSubscription subscriptions = new CompositeSubscription();

		public EventualPathImpl(List<Object> segments) {
			this.segments = segments;
			Cloud.this.subscriptions.add(subscriptions);
		}

		@Override
		public Observable<PathEvent> children() {
			return Observable.create(new OnSubscribe<PathEvent>() {
				@Override
				public void call(final Subscriber<? super PathEvent> subscriber) {
					Subscription subscription = eventualCloud.subscribe(new Action1<CloudConnection>() {
						@Override
						public void call(CloudConnection cloud) {
							subscriber.onNext(new PathEvent(cloud.path(segments)));
						}
					});
					subscriptions.add(subscription);
					subscriber.add(Subscriptions.create(new Action0() {
						@Override
						public void call() {
							dispose();
						}
					}));
				}
			});
		}

		@Override
		public EventualPath append(Object segment) {
			return path(Path.append(segments, segment));
		}

		public void dispose() {
			Cloud.this.subscriptions.remove(subscriptions);
			subscriptions.unsubscribe();
		}

		@Override
		public void pub() {
			Subscription subscription = eventualCloud.subscribe(new Action1<CloudConnection>() {
				@Override
				public void call(CloudConnection cloud) {
					cloud.path(segments).pub();
				}
			});
			subscriptions.add(subscription);
		}

		@Override
		public void pub(final Object value) {
			Subscription subscription = eventualCloud.subscribe(new Action1<CloudConnection>() {
				@Override
				public void call(CloudConnection cloud) {
					cloud.path(segments).pub(value);
				}
			});
			subscriptions.add(subscription);
		}

		@Override
		public Observable<Object> value() {
			return Observable.create(new OnSubscribe<Object>() {

				@Override
				public void call(final Subscriber<? super Object> subscriber) {
					Subscription subscription = eventualCloud.subscribe(new Action1<CloudConnection>() {
						@Override
						public void call(CloudConnection cloud) {
							final Subscription valueSubscription = cloud.path(segments).value().subscribe(new Action1<Object>() {
								@Override
								public void call(Object value) {
									subscriber.onNext(value);
								}
							});
							subscriber.add(Subscriptions.create(new Action0() {
								@Override
								public void call() {
									valueSubscription.unsubscribe();
								}
							}));
						}
					});
					subscriptions.add(subscription);
					subscriber.add(Subscriptions.create(new Action0() {
						@Override
						public void call() {
							dispose();
						}
					}));
				}
			});
		}
	}

	private Observable<CloudConnection> eventualCloud;
	private CompositeSubscription subscriptions = new CompositeSubscription(); 

	public static Cloud cloudFor(final Context context) {
		return new Cloud(CloudServiceConnection.cloudFor(context).publish().refCount());
	}

	public Cloud(Observable<CloudConnection> eventualCloud) {
		this.eventualCloud = eventualCloud;
	}

	public void dispose() {
		subscriptions.unsubscribe();
		subscriptions.clear();
	}

	public EventualPath path(Object... segments) {
		return path(Arrays.asList(segments));
	}

	public EventualPath path(List<Object> list) {
		return new EventualPathImpl(list);
	}

}
