package sneer.android.main;

import static sneer.snapi.Cloud.*;
import static sneer.snapi.CloudPath.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Scheduler.Inner;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.observables.*;
import rx.schedulers.*;
import rx.subjects.*;
import rx.subscriptions.*;
import sneer.api.*;
import sneer.cloud.client.*;
import sneer.cloud.client.Cloud;
import sneer.cloud.client.Subscriber;
import sneer.cloud.client.Subscription;
import sneer.cloud.client.impl.*;
import sneer.keys.*;
import sneer.snapi.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.widget.*;
import basis.*;

public class CloudService extends Service {
	
	private rx.Subscription cloudMasterSubscription;
	private Subject<CloudMaster, CloudMaster> observableCloudMaster = ReplaySubject.create();
	private ReplaySubject<sneer.snapi.Cloud> notificationConnection = ReplaySubject.create();
	private Map<List<Object>, rx.Subscription> notificationRegistrar = new HashMap<List<Object>, rx.Subscription>();

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		@SuppressWarnings("unused")
		Bundle bundle;
		if (intent != null && (bundle = intent.getExtras()) != null) {
//			handleMessage(bundle);
		}
		
        return START_STICKY;
	}
	

	@SuppressWarnings("unused")
	private void handleMessage(Bundle bundle) {
		
		List<Value> path = bundle.getParcelableArrayList("path");
		
		int op = bundle.getInt("op");
		switch (op) {
		case REGISTER_NOTIFICATION:
			registerForNotification(bundle.<Intent>getParcelable("launch"), Encoder.pathDecode(path.toArray(new Value[path.size()])));
			break;
		case UNREGISTER_NOTIFICATION:
			unregisteForNotification(Encoder.pathDecode(path.toArray(new Value[path.size()])));
			break;
		default:
			throw new RuntimeException("Unknown option: " + op);
		}
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
//		NetworkImpl.overrideServerHost("192.168.56.1"); //Typical VirtualBox (Genymotion) gateway address to host machine
		
		final Subject<String, String> errors = PublishSubject.create();
		
		errors.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {@Override public void call(String error) {
			toast(error);
			Log.e(CloudService.class.getSimpleName(), error);
		}});
		
		ConnectableObservable<CloudMaster> connectableCloudMaster = Observable.create(new OnSubscribe<CloudMaster>() {@Override public void call(final rx.Subscriber<? super CloudMaster> subscriber) {
				try {
					final CloudMaster master = new CloudMasterImpl(publicKey(), new Consumer<String>() {@Override public void consume(String error) {
						errors.onNext(error);
					}});
					subscriber.add(Subscriptions.create(new Action0() {@Override public void call() {
						master.close();
					}}));
					subscriber.onNext(master);
				} catch (SocketException e) {
					subscriber.onError(e);
				}
				
			}})
			.subscribeOn(Schedulers.newThread())
			.onErrorResumeNext(new Func1<Throwable, Observable<? extends CloudMaster>>() {@Override public Observable<? extends CloudMaster> call(Throwable error) {
				errors.onNext(error.getMessage());
				return Observable.<CloudMaster>never().timeout(5, TimeUnit.SECONDS);
			}}).retry().publish();
		
		connectableCloudMaster.first().subscribe(observableCloudMaster);
		
		cloudMasterSubscription = connectableCloudMaster.connect();
		
		AndroidSchedulers.mainThread().schedule(new Action1<Scheduler.Inner>() {@Override public void call(Inner arg0) {
			notificationConnection.onNext(sneer.snapi.Cloud.onAndroidMainThread(CloudService.this));
			notificationConnection.onCompleted();
		}});
	}
	
	
	private void toast(String error) {
		Toast.makeText(CloudService.this, error, Toast.LENGTH_LONG).show();
	}
	
	
	@Override
	public void onDestroy() {
		cloudMasterSubscription.unsubscribe();
		super.onDestroy();
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
	    final String caller = getPackageManager().getNameForUid(Binder.getCallingUid());
		final Subject<Cloud, Cloud> observableCloud = ReplaySubject.create();
		this.observableCloudMaster.map(new Func1<CloudMaster, Cloud>() {@Override public Cloud call(CloudMaster master) {
			return master.freshCloudFor(caller);
		}}).subscribe(observableCloud);
		
		return new ICloud.Stub() {
			
			@Override
			public void pubPath(final Value[] path) throws RemoteException {
				withCloud(new Action1<Cloud>() {@Override public void call(Cloud cloud) {
					cloud.pubPath(Encoder.pathDecode(path));					
				}});
			}
			
			private rx.Subscription withCloud(Action1<Cloud> action) {
				return observableCloud.first().subscribe(action);
			}

			@Override
			public void pubValue(final Value[] path, final Value value) throws RemoteException {
				withCloud(new Action1<Cloud>() {@Override public void call(Cloud cloud) {
					cloud.pubValue(Encoder.pathDecode(path), value.get());					
				}});
			}

			@Override
			public ISubscription sub(final Value[] path, final ISubscriber subscriber) throws RemoteException {
				
				final CompositeSubscription subscription = new CompositeSubscription(); 
				
				subscription.add(withCloud(new Action1<Cloud>() {@Override public void call(Cloud cloud) {
						
					final Subscription networkSubscription = cloud.sub(Encoder.pathDecode(path), new Subscriber() {
						@Override
						public void onPath(List<Object> path) throws Exception {
							subscriber.onPath(Encoder.pathEncode(path));
						}

						@Override
						public void onValue(List<Object> path, Object value) throws Exception {
							subscriber.onValue(Encoder.pathEncode(path), Value.of(value));
						}
					});
					subscription.add(Subscriptions.create(new Action0() {@Override public void call() {
						networkSubscription.dispose();
					}}));
				}}));

				
				return new ISubscription.Stub() {
					@Override
					public void dispose() throws RemoteException {
						subscription.unsubscribe();
					}
				};
			}

			@Override
			public byte[] ownPublicKey() throws RemoteException {
				return publicKey().bytes();
			}
		};
	}
	

	private PublicKey publicKey() {
		KeyStore.initKeys(getApplicationContext());
		return PublicKey.fromByteArray(KeyStore.publicKey());
	}
	
	
	private void registerForNotification(final Intent intent, final List<Object> segments) {

		rx.Subscription old;
		if ((old = notificationRegistrar.remove(segments)) != null) {
			old.unsubscribe();
		}
		
		final CompositeSubscription sub = new CompositeSubscription();
		sub.add(notificationConnection.first().subscribe(new Action1<sneer.snapi.Cloud>() {@Override public void call(final sneer.snapi.Cloud cloud) {
			sub.add(cloud.contacts(ME).flatMap(new Func1<Contact, Observable<PathEvent>>() {@Override public Observable<PathEvent> call(Contact contact) {
				List<Object> path = new ArrayList<Object>(segments);
				path.add(0, contact.publicKey());
				return cloud.path(path).children();
			}})
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(new Action1<PathEvent>() {@Override public void call(PathEvent arg0) {
				notifyUser(segments, intent);
			}}));
		}}));
		
		notificationRegistrar.put(segments, sub);
	}
	

	private void notifyUser(List<Object> segments, Intent intent) {
		
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
	    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	    builder
	    		.setSmallIcon(R.drawable.ic_launcher)
	            .setContentText(segments.toString())
	            .setContentTitle("Sneer")
	            .setWhen(System.currentTimeMillis())
	            .setAutoCancel(true)
	            .setOngoing(false)
	            .setContentIntent(pendIntent);
	    
	    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(segments.hashCode(), builder.build());
	}
	
	
	private void unregisteForNotification(List<Object> path) {
		notificationRegistrar.remove(path);
	}
	

	@SuppressWarnings("unused")
	static private void log(String message) {
		Log.d(CloudService.class.getSimpleName(), message);
	}
	
}
