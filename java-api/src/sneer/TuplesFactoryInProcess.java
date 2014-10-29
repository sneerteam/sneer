package sneer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;
import sneer.refimpl.LocalTuplesFactory;
import sneer.tuples.Tuple;

public class TuplesFactoryInProcess extends LocalTuplesFactory {
	
	Subject<Tuple, Tuple> tuples = ReplaySubject.create();

	@Override
	protected void publishTuple(Tuple tuple) {
		tuples.onNext(tuple);
	}

	@Override
	protected Observable<Tuple> query(final PrivateKey identity, Map<String, Object> criteria) {
		Observable<Tuple> t = tuples;
		if (!criteria.containsKey("audience")) {
			t = t.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple tuple) {
				return tuple.audience() == null || tuple.author().equals(identity.publicKey()) || tuple.audience().equals(identity.publicKey());
			}});
		}
		
		for (final Entry<String, Object> criterion : criteria.entrySet()) {
			t = t.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple tuple) {
				String key = criterion.getKey();
				Object tupleValue = tuple.get(key);
				Object value = criterion.getValue();
				return tupleValue == null
					? value == null
					: (value != null && value.getClass().isArray() ? Arrays.equals((Object[])value, (Object[])tupleValue) : tupleValue.equals(value));
			}});
		}
		return t;
	}
	
	@Override
	protected Observable<Tuple> queryLocal(final PrivateKey identity, final Map<String, Object> criteria) {
		return Observable.create(new OnSubscribe<Tuple>() { @Override public void call(final Subscriber<? super Tuple> subs) {
			final ReplaySubject<Subscription> s = ReplaySubject.create();
			s.onNext(query(identity, criteria)
				.buffer(100, TimeUnit.MILLISECONDS)
				.subscribe(new Action1<List<Tuple>>() { @Override public void call(List<Tuple> list) {
					subs.add(Observable.from(list).subscribe(subs));
					s.subscribe(new Action1<Subscription>() {  @Override public void call(Subscription subscription) {
						subscription.unsubscribe();
					}});
				}}));
		}});
	}
	
}
