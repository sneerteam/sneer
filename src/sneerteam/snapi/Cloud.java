package sneerteam.snapi;

import java.util.*;

import rx.*;
import rx.Observable;
import rx.functions.*;
import rx.observables.*;
import rx.subjects.*;
import android.content.*;
import android.util.*;

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
		return new Cloud(CloudServiceConnection.cloudFor(context).publish());
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

	private static void log(String log) {
		Log.d(CloudPathImpl.class.getSimpleName(), log);
	}
}
