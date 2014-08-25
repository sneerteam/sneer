package sneer;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.refimpl.*;
import sneer.tuples.*;

public class TuplesFactoryInProcess extends LocalTuplesFactory {
	
	Subject<Tuple, Tuple> tuples = ReplaySubject.create();

	@Override
	protected void publishTuple(Tuple tuple) {
		tuples.onNext(tuple);
	}

	@Override
	protected Observable<Tuple> query(final PrivateKey identity, Map<String, Object> criteria) {
		Observable<Tuple> t = tuples;
		for (final Entry<String, Object> criterion : criteria.entrySet()) {
			t = t.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
				String key = criterion.getKey();
				Object tupleValue = t1.get(key);
				Object value = criterion.getValue();
				if (key.equals("audience")) {
					return tupleValue == null || tupleValue.equals(identity.publicKey()) || t1.author().equals(identity.publicKey());
				}
				return tupleValue == null
					? value == null
					: (value != null && value.getClass().isArray() ? Arrays.equals((Object[])value, (Object[])tupleValue) : tupleValue.equals(value));
			}});
		}
		return t;
	}
	
	@Override
	protected Observable<Tuple> queryLocal(final PrivateKey identity, final Map<String, Object> criteria) {
		return Observable.create(new OnSubscribe<Tuple>() { @Override public void call(final Subscriber<? super Tuple> t1) {
			query(identity, criteria)
				.buffer(100, TimeUnit.MILLISECONDS)
				.subscribe(new Action1<List<Tuple>>() { @Override public void call(List<Tuple> list) {
					t1.add(Observable.from(list).subscribe(t1));
				} });
		}});
	}
	
	
	
}
