package sneerteam.snapi;

import java.util.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Scheduler.Inner;
import rx.Observable;
import rx.functions.*;
import rx.subscriptions.*;
import sneerteam.api.*;
import android.os.*;

public class Path {

	private final CloudConnection cloudConnection;
	private final List<Object> segments;

	Path(CloudConnection cloudConnection, List<Object> segments) {
		this.cloudConnection = cloudConnection;
		this.segments = segments;
	}
	
	public void pub() {
		try {
			cloud().pubPath(path());
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void pub(Object value) {
		try {
			cloud().pubValue(path(), Value.of(value));
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public Path append(Object segment) {
		return new Path(cloudConnection, append(segments, segment));
	}
	
	public Path prepend(Object segment) {
		return new Path(cloudConnection, prepend(segments, segment));
	}
	
	public rx.Observable<PathEvent> children() {
		return Observable.create(new OnSubscribe<PathEvent>() {@Override public void call(final Subscriber<? super PathEvent> subscriber) {
			try {
				final ISubscription sub = sub(new ISubscriber() {
					@Override
					public void onPath(final Value[] path) {
						cloudConnection.scheduler().schedule(new Action1<Scheduler.Inner>() {@Override public void call(Inner arg0) {
							subscriber.onNext(
									new PathEvent(cloudConnection.path(Encoder.pathDecode(path))));
						}});
					}

					@Override
					public void onValue(Value[] path, Value value) {
					}
					
					@Override
					public IBinder asBinder() {
						return null;
					}
				});
				
				if (sub != null)
					subscriber.add(Subscriptions.create(new Action0() {@Override public void call() {
						try {
							sub.dispose();
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}}));
				
			} catch (RemoteException e) {
				e.printStackTrace();
				subscriber.onError(e);
			}
		}});
	}
	
	public rx.subjects.Subject<Object, Object> value() {
		return new rx.subjects.Subject<Object, Object>(new OnSubscribe<Object>() {@Override public void call(final Subscriber<? super Object> subscriber) {
			try {
				final ISubscription sub = sub(new ISubscriber() {
					@Override
					public void onPath(Value[] path) {
					}

					@Override
					public void onValue(Value[] path, final Value value) {
						cloudConnection.scheduler().schedule(new Action1<Scheduler.Inner>() {@Override public void call(Inner arg0) {
							subscriber.onNext(value.get());
						}});
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
				e.printStackTrace();
				subscriber.onError(e);
			}
		}}) {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
			}

			@Override
			public void onNext(Object value) {
				pub(value);
			}};
	}
	
	private ISubscription sub(ISubscriber subscriber) throws RemoteException {
		return cloudConnection.sub(path(), subscriber); 
	}

	private Value[] path() {
		return Encoder.pathEncode(segments);
	}

	private ICloud cloud() {
		return cloudConnection.cloud;
	}

	public static List<Object> append(List<Object> segments, Object segment) {
		ArrayList<Object> result = new ArrayList<Object>(segments.size() + 1);
		result.addAll(segments);
		result.add(segment);
		return result;
	}	
	
	public static List<Object> prepend(List<Object> segments, Object segment) {
		ArrayList<Object> result = new ArrayList<Object>(segments.size() + 1);
		result.add(segment);
		result.addAll(segments);
		return result;
	}

    public Object lastSegment() {
        return segments.get(segments.size()-1);
    }

    public List<Object> segments() {
		return segments;
	}
    
    public Path parent() {
    	return new Path(cloudConnection, segments.subList(0, segments.size()-1));
    }
}
