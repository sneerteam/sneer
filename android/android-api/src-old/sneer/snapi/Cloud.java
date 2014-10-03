package sneer.snapi;

import static sneer.snapi.CloudPath.*;

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
import sneer.api.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class Cloud {
	
	static final String NOTIFICATIONS = "notifications";
	
	private final class NotificationPathImpl implements NotificationPath {
		
		private static final String PAYLOAD = "payload";
		private static final String TIMESTAMP = "timestamp";
		private static final String CONTENT_TEXT = "content-text";
		
		private Object[] segments;
		private Class<? extends Activity> activity;

		public NotificationPathImpl(Class<? extends Activity> activity, Object... segments) {
			this.activity = activity;
			this.segments = segments;
		}
		
		


		@Override
		public Observable<CloudNotification> notifications() {
//			return contacts().flatMap(new Func1<Contact, Observable<CloudNotification>>() {  @Override public Observable<CloudNotification> call(final Contact contact) {
//				return path(contact.publicKey(), NOTIFICATIONS).appends(segments).children().flatMap(new Func1<PathEvent, Observable<CloudNotification>>() {  @Override public Observable<CloudNotification> call(PathEvent idPath) {
//					return path(idPath.path()).value().map(new Func1<Object, CloudNotification>() {  @Override public CloudNotification call(Object notification) {
//						return asCloudNotification(contact, notification);
//					}});
//				}});
//			}});
			return Observable.create(new OnSubscribe<CloudNotification>() {
				@Override
				public void call(Subscriber<? super CloudNotification> subscriber) {
					Intent launcher = new Intent(context, activity);
					launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
					Intent intent = new Intent("sneer.intent.action.BIND_CLOUD_SERVICE");
				    intent.setClassName("sneer.android.main", "sneer.android.main.CloudService");
				    intent.putExtra("op", REGISTER_NOTIFICATION);
				    intent.putParcelableArrayListExtra("path", asParceableValue(segments));
				    intent.putExtra("launch", launcher);
				        
				    context.startService(intent);					
				}
			});
		}


		@Override
		public void pub(String receiverPuk, CharSequence contentText, Object payload) {
			CloudPath path = path(NOTIFICATIONS).append(receiverPuk).appends(segments);
			path.pub(asMap(contentText, Clock.now(), payload));
		}


		private Map<String, Object> asMap(CharSequence contentText, long timestamp, Object payload) {
			Map<String, Object> ret = new HashMap<String, Object>();
			ret.put(CONTENT_TEXT, contentText);
			ret.put(TIMESTAMP, timestamp);
			ret.put(PAYLOAD, payload);
			return ret;
		}


		@SuppressWarnings("unused")
		private CloudNotification asCloudNotification(Contact contact, Object notificationObj) {
			@SuppressWarnings("unchecked")
			Map<String, Object> notification = (Map<String, Object>) notificationObj;
			return new CloudNotification(contact, (CharSequence)notification.get(CONTENT_TEXT), (Long)notification.get(TIMESTAMP), notification.get(PAYLOAD));
		}


	}
	
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
		public CloudPath appends(Object... newSegments) {
			return path(Path.appends(segments, newSegments));
		}

		@Override
		public CloudPath appends(List<Object> newSegments) {
			return path(Path.appends(segments, newSegments));
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

	public static final int REGISTER_NOTIFICATION = 1;
	public static final int UNREGISTER_NOTIFICATION = 2;

	private ReplaySubject<CloudConnection> eventualCloud;
	private Subscription subscription;
	private Context context;

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
		return new Cloud(context, CloudServiceConnection.cloudFor(context, scheduler).publish());
	}
	

	public Cloud(Context context, ConnectableObservable<CloudConnection> eventualCloud) {
		this.context = context;
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
	
	
	public NotificationPath notificationPath(Class<? extends Activity> activity, Object... segments) {
		return new NotificationPathImpl(activity, segments);
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
	
	public Observable<Contact> contacts() {
        return contacts(ME);
	    
	}

    public Observable<Contact> contacts(Object root) {
        return path(root, "contacts").children()
                .flatMap(new Func1<PathEvent, Observable<Contact>>() {@Override public Observable<Contact> call(final PathEvent event) {
                    return event.path().append("nickname").value().map(new Func1<Object, Contact>() {@Override public Contact call(Object nickname) {
                        return new Contact((String) event.path().lastSegment(), (String) nickname);
                    }});
                }});
    }
    
    public void registerForNotification(final Intent launch, final Object... segments) {
        Intent intent = new Intent("sneer.intent.action.BIND_CLOUD_SERVICE");
        intent.setClassName("sneer.android.main", "sneer.android.main.CloudService");
        intent.putExtra("op", REGISTER_NOTIFICATION);
        intent.putParcelableArrayListExtra("path", asParceableValue(segments));
        intent.putExtra("launch", launch);
        
        context.startService(intent);
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ArrayList<Value> asParceableValue(final Object... segments) {
		return new ArrayList(Arrays.asList(Encoder.pathEncode(Arrays.asList(segments))));
	}
    
    public void unregisterForNotification(final Object... segments) {
		Intent intent = new Intent("sneer.intent.action.BIND_CLOUD_SERVICE");
		intent.setClassName("sneer.android.main", "sneer.android.main.CloudService");
		intent.putExtra("op", UNREGISTER_NOTIFICATION);
		intent.putParcelableArrayListExtra("path", asParceableValue(segments));
		
		context.startService(intent);
    }

}
