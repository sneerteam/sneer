package sneer.android.main;

import static sneer.SneerAndroid.SubscriptionOp.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.functions.*;
import sneer.*;
import sneer.SneerAndroid.*;
import sneer.api.*;
import sneer.tuples.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class SneerAndroidService extends Service {
	
	private AtomicInteger nextSubscriptionId = new AtomicInteger();
	private ConcurrentMap<Integer, Subscription> subscriptions = new ConcurrentHashMap<Integer, Subscription>();

	@SuppressWarnings("unchecked")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		switch((SneerAndroid.TupleSpaceOp)intent.getSerializableExtra("op")) {
		case PUBLISH:
			publish((Map<String, Object>)intent.getSerializableExtra("tuple"));
			break;
		case SUBSCRIBE:
			subscribe((Map<String, Object>)intent.getSerializableExtra("criteria"), (ResultReceiver)intent.getParcelableExtra("resultReceiver"));
			break;
		case UNSUBSCRIBE:
			unsubscribe(intent.getIntExtra("subscription", -1));
			break;
		default:
			break;
		}
		
		return START_STICKY;
	}
	
	private void unsubscribe(int subscriptionId) {
		if (subscriptionId == -1) {
			throw new IllegalArgumentException("Unknown subscription: " + subscriptionId);
		}
		subscriptions.remove(subscriptionId).unsubscribe();
	}

	private void subscribe(Map<String, Object> criteria, final ResultReceiver resultReceiver) {
		Observable<Tuple> tuples = SneerSingleton.sneer().tupleSpace().filter().putFields(criteria).tuples();
		int id = nextSubscriptionId.getAndIncrement();
		resultReceiver.send(SUBSCRIPTION_ID.ordinal(), bundle(id));
		
		Subscription s = tuples.doOnCompleted(new Action0() {  @Override public void call() {
			resultReceiver.send(ON_COMPLETED.ordinal(), new Bundle());
		}}).subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
			resultReceiver.send(ON_NEXT.ordinal(), bundle(new HashMap<String, Object>(t1)));
		} });
		
		subscriptions.put(id, s);
	}

	private Bundle bundle(Object value) {
		Bundle ret = new Bundle();
		if (value instanceof Parcelable) {
			ret.putParcelable("value", (Parcelable) value);
		} else {
			ret.putSerializable("value", (Serializable) value);
		}
		return ret;
	}

	private void publish(Map<String, Object> tuple) {
		SneerSingleton.sneer().tupleSpace().publisher().putFields(tuple).pub();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
