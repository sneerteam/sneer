package sneer;

import java.util.*;

import rx.*;
import rx.Scheduler.Inner;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.subscriptions.*;
import sneer.commons.*;
import sneer.refimpl.*;
import sneer.tuples.*;
import android.content.*;
import android.os.*;

public class TupleSpaceFactoryClient extends LocalTuplesFactory {
	
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

	private Context context;
	private InteractiveSerializer serializer = new InteractiveSerializer();

	public TupleSpaceFactoryClient(Context context) {
		this.context = context;
	}

	@Override
	protected void publishTuple(final Tuple ret) {
		AndroidSchedulers.mainThread().schedule(new Action1<Scheduler.Inner>() {  @Override public void call(Inner t1) {
			Intent intent = new Intent(SneerAndroid.SNEER_SERVICE);
			intent.putExtra("op", TupleSpaceOp.PUBLISH);
			intent.putExtra("tuple", serializer.serialize(ret));
			context.startService(intent);
		}});
	}

	@Override
	protected Observable<Tuple> query(PrivateKey identity, final Map<String, Object> criteria) {
		return Observable.create(new Observable.OnSubscribe<Tuple>() {  @Override public void call(final Subscriber<? super Tuple> subscriber) {
			Intent intent = new Intent(SneerAndroid.SNEER_SERVICE);
			intent.putExtra("op", TupleSpaceFactoryClient.TupleSpaceOp.SUBSCRIBE);
			intent.putExtra("criteria", serializer.serialize(criteria));
			intent.putExtra("resultReceiver", new ResultReceiver(null) {  @SuppressWarnings("unchecked") @Override protected void onReceiveResult(int resultCode, Bundle resultData) {
				switch (TupleSpaceFactoryClient.SubscriptionOp.values()[resultCode]) {
				case ON_COMPLETED:
					subscriber.onCompleted();
					break;
				case ON_NEXT:
					subscriber.onNext(newTupleFromMap((Map<String, Object>) serializer.deserialize((byte[]) SneerAndroid.unbundle(resultData))));
					break;
				case SUBSCRIPTION_ID: {
					final int subscriptionId = (Integer) SneerAndroid.unbundle(resultData);
					subscriber.add(Subscriptions.create(new Action0() { @Override public void call() {
						Intent intent = new Intent(SneerAndroid.SNEER_SERVICE);
						intent.putExtra("op", TupleSpaceFactoryClient.TupleSpaceOp.UNSUBSCRIBE);
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

}