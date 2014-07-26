package sneer;

import java.io.*;
import java.util.*;

import rx.*;
import rx.Observable;
import rx.Scheduler.*;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.subscriptions.*;
import sneer.api.*;
import sneer.refimpl.*;
import sneer.rx.*;
import sneer.snapi.*;
import sneer.snapi.Contact;
import sneer.tuples.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class SneerAndroid {

	private static final String INTERACTION_LIST = "sneer.android.main.INTERACTION_LIST";
	public static final String TITLE = "title";
	public static final String TYPE = "type";
	public static final String NEW_INTERACTION_LABEL = "newInteractionLabel";
	public static final String NEW_INTERACTION_ACTION = "newInteractionAction";
	public static final String DISABLE_MENUS = "disable-menus";
	private static final String SNEER_SERVICE = "sneer.android.service.BACKEND";
	
	public enum TupleSpaceOp {
		PUBLISH,
		SUBSCRIBE,
		UNSUBSCRIBE
	}
	
	public enum SubscriptionOp {
		SUBSCRIPTION_ID,
		ON_NEXT,
		ON_COMPLETED
	}

	public static void startInteractionList(Activity activity, String title, String type, String newInteractionLabel, String newInteractionAction) {
		try {
			Intent intent = new Intent(INTERACTION_LIST);
			intent.putExtra(TITLE, title);
			intent.putExtra(TYPE, type);
			intent.putExtra(NEW_INTERACTION_LABEL, newInteractionLabel);
			intent.putExtra(NEW_INTERACTION_ACTION, newInteractionAction);
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			SneerUtils.showInstallSneerDialog(activity);
		}
	}
	
	public static <T> Session<T> sessionOnAndroidMainThread(Activity activity) {
		return new SneerAndroid(activity).getSession();
	}
	
	private Context context;

	class TupleSpaceFactoryClient extends LocalTuplesFactory {

		@Override
		protected void publishTuple(final Tuple ret) {
			AndroidSchedulers.mainThread().schedule(new Action1<Scheduler.Inner>() {  @Override public void call(Inner t1) {
				Intent intent = new Intent(SNEER_SERVICE);
				intent.putExtra("op", TupleSpaceOp.PUBLISH);
				intent.putExtra("tuple", serialize(ret));
				context.startService(intent);
			}});
		}

		private byte[] serialize(Object obj) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				ObjectOutputStream out = new ObjectOutputStream(bytes);
				out.writeObject(obj);
				out.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return bytes.toByteArray();
		}

		private Object deserialize(byte[] bytes) {
			try {
				return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
			} catch (OptionalDataException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected Observable<Tuple> query(PrivateKey identity, final Map<String, Object> criteria) {
			return Observable.create(new Observable.OnSubscribe<Tuple>() {  @Override public void call(final Subscriber<? super Tuple> subscriber) {
				Intent intent = new Intent(SNEER_SERVICE);
				intent.putExtra("op", TupleSpaceOp.SUBSCRIBE);
				intent.putExtra("criteria", serialize(criteria));
				intent.putExtra("resultReceiver", new ResultReceiver(null) {
						
					private int subscriptionId = -1;

					@SuppressWarnings("unchecked")
					@Override
					protected void onReceiveResult(int resultCode, Bundle resultData) {
						switch (SubscriptionOp.values()[resultCode]) {
						case ON_COMPLETED:
							subscriber.onCompleted();
							break;
						case ON_NEXT:
							subscriber.onNext(newTupleFromMap((Map<String, Object>) deserialize((byte[]) unbundle(resultData))));
							break;
						case SUBSCRIPTION_ID:
							subscriptionId = (Integer) unbundle(resultData);
							break;
						default:
							break;
						}
						subscriber.add(Subscriptions.create(new Action0() { @Override public void call() {
							if (subscriptionId == -1) {
								return;
							}
							Intent intent = new Intent(SNEER_SERVICE);
							intent.putExtra("op", TupleSpaceOp.UNSUBSCRIBE);
							intent.putExtra("subscription", subscriptionId);
							context.startService(intent);
						}}));
					}
				});
				context.startService(intent);
			}}).observeOn(AndroidSchedulers.mainThread()).subscribeOn(AndroidSchedulers.mainThread());
		}

	}
	
	private static Object unbundle(Bundle resultData) {
		return resultData.get("value");
	}
	
	public SneerAndroid(Context context) {
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Session<T> getSession() {
		if (!(context instanceof Activity)) {
			throw new IllegalStateException("Context expected to be an Activity, found " + context.getClass().getName());
		}
		
		final PrivateKey myPrivateKey = (PrivateKey) getExtra("myPrivateKey");
		final TupleSpace tupleSpace = new TupleSpaceFactoryClient().newTupleSpace(myPrivateKey);
		
		return new Session<T>() {

			@Override
			public Observed<String> contactNickname() {
				return new Observed<String>() {
					
					@Override
					public Observable<String> observable() {
						throw new RuntimeException("not implemented yet");
					}
					
					@Override
					public String mostRecent() {
						return (String) getExtra("contactNickname");
					}
				};
			}

			@Override
			public void send(T value) {
				tupleSpace.publisher()
					.audience(partyPublicKey())
					.type(type())
					.pub(value);
			}

			private String type() {
				return (String) getExtra(TYPE);
			}

			@Override
			public Observable<T> received() {
				return (Observable<T>) tupleSpace.filter()
						.audience(myPrivateKey)
						.author(partyPublicKey())
						.type(type())
						.tuples()
						.observeOn(AndroidSchedulers.mainThread())
						.subscribeOn(AndroidSchedulers.mainThread())
						.map(Tuple.TO_PAYLOAD);
						
			}

			@Override
			public void dispose() {
				// TODO
			}
			
			private PublicKey partyPublicKey() {
				return (PublicKey) getExtra("contactPuk");
			}
			
		};
	}
	
	private Object getExtra(String key) {
		return ((Activity)context).getIntent().getExtras().get(key);
	}

}
