package sneer;

import java.util.*;

import rx.*;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.subscriptions.*;
import sneer.commons.*;
import sneer.refimpl.*;
import sneer.tuples.*;
import android.content.*;
import android.os.*;
import android.util.*;

public class TupleSpaceFactoryClient extends LocalTuplesFactory {
	
	public static final String TUPLE_SPACE_SERVICE = "sneer.android.service.TUPLE_SPACE";

	public enum TupleSpaceOp {
		PUBLISH,
		SUBSCRIBE,
		SUBSCRIBE_LOCAL,
		UNSUBSCRIBE
	}

	public enum SubscriptionOp {
		SUBSCRIPTION_ID,
		ON_NEXT,
		ON_COMPLETED
	}

	private Context context;
	private InteractiveSerializer serializer = new InteractiveSerializer();

	public TupleSpaceFactoryClient(Context context) {
		this.context = context;
	}

	@Override
	protected void publishTuple(final Tuple ret) {
		AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {  @Override public void call() {
			Intent intent = new Intent(TUPLE_SPACE_SERVICE);
			intent.putExtra("op", TupleSpaceOp.PUBLISH.ordinal());
			intent.putExtra("tuple", serializer.serialize(ret));
			context.startService(intent);
		}});
	}

	@Override
	protected Observable<Tuple> query(PrivateKey identity, final Map<String, Object> criteria) {
		return query(TupleSpaceFactoryClient.TupleSpaceOp.SUBSCRIBE, criteria);
	}

	@Override
	protected Observable<Tuple> queryLocal(PrivateKey identity, Map<String, Object> criteria) {
		return query(TupleSpaceFactoryClient.TupleSpaceOp.SUBSCRIBE_LOCAL, criteria);
	}
	
	private Observable<Tuple> query(final TupleSpaceOp op, final Map<String, Object> criteria) {
		return Observable.create(new Observable.OnSubscribe<Tuple>() {  @Override public void call(final Subscriber<? super Tuple> subscriber) {
			Intent intent = new Intent(TUPLE_SPACE_SERVICE);
			intent.putExtra("op", op.ordinal());
			log("tupleSpace: subscribe: " + criteria);
			intent.putExtra("criteria", serializer.serialize(criteria));
			intent.putExtra("resultReceiver", new ResultReceiver(null) {  @SuppressWarnings("unchecked") @Override protected void onReceiveResult(int resultCode, Bundle resultData) {
				switch (TupleSpaceFactoryClient.SubscriptionOp.values()[resultCode]) {
				case ON_COMPLETED:
					log("tupleSpace: completed");
					subscriber.onCompleted();
					break;
				case ON_NEXT: {
					Tuple tuple = newTupleFromMap((Map<String, Object>) serializer.deserialize((byte[]) SneerAndroid.unbundle(resultData)));
					log("tupleSpace: tuple: " + tuple);
					subscriber.onNext(tuple);
					break;
				}
				case SUBSCRIPTION_ID: {
					final int subscriptionId = (Integer) SneerAndroid.unbundle(resultData);
					log("tupleSpace: subscriptionId: " + subscriptionId);
					subscriber.add(Subscriptions.create(new Action0() { @Override public void call() {
						Intent intent = new Intent(TUPLE_SPACE_SERVICE);
						intent.putExtra("op", TupleSpaceFactoryClient.TupleSpaceOp.UNSUBSCRIBE.ordinal());
						intent.putExtra("subscription", subscriptionId);
						context.startService(intent);
					}}));
					break;
				}
				default:
					break;
				}
			}});
			context.startService(intent);
		}}).observeOn(AndroidSchedulers.mainThread()).subscribeOn(AndroidSchedulers.mainThread());
	}

	private void log(String message) {
		Log.i(TuplesFactoryInProcess.class.getSimpleName(), message);
	}

}