package sneerteam.snapi;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class Path {

	private CloudConnection cloudConnection;
	private List<Object> segments;

	Path(CloudConnection cloudConnection, List<Object> segments) {
		this.cloudConnection = cloudConnection;
		this.segments = segments;
	}
	
	public Path append(Object segment) {
		return new Path(cloudConnection, append(segments, segment));
	}
	
	public rx.Observable<PathEvent> children() {
		return Observable.create(new OnSubscribe<PathEvent>() {@Override public void call(final Subscriber<? super PathEvent> subscriber) {
			try {
				final ISubscription sub = cloudConnection.cloud.sub(Encoder.pathEncode(segments), new ISubscriber() {
					@Override
					public void onPath(Bundle[] path) {
						subscriber.onNext(
								new PathEvent(cloudConnection.path(Encoder.pathDecode(path))));
					}

					@Override
					public void onValue(Bundle[] path, Bundle value) {
					}
					
					@Override
					public IBinder asBinder() {
						return null;
					}
				});
				
				subscriber.add(Subscriptions.create(new Action0() {@Override public void call() {
					try {
						sub.dispose();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}}));
				
			} catch (RemoteException e) {
				subscriber.onError(e);
			}
		}});
	}
	
	public rx.Observable<Object> value() {
		return Observable.create(new OnSubscribe<Object>() {@Override public void call(final Subscriber<? super Object> subscriber) {
			try {
				final ISubscription sub = cloudConnection.cloud.sub(Encoder.pathEncode(segments), new ISubscriber() {
					@Override
					public void onPath(Bundle[] path) {
					}

					@Override
					public void onValue(Bundle[] path, Bundle value) {
						subscriber.onNext(Encoder.unbundle(value));
					}
					
					@Override
					public IBinder asBinder() {
						return null;
					}
				});
				
				subscriber.add(Subscriptions.create(new Action0() {@Override public void call() {
					try {
						sub.dispose();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}}));
				
			} catch (RemoteException e) {
				subscriber.onError(e);
			}
		}});

	}
	
	static List<Object> append(List<Object> segments, Object segment) {
		ArrayList<Object> result = new ArrayList<Object>(segments.size() + 1);
		result.addAll(segments);
		result.add(segment);
		return result;
	}	
}
