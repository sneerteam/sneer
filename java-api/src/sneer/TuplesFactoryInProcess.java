package sneer;

import java.util.*;
import java.util.Map.Entry;

import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.refimpl.*;
import sneer.tuples.*;

public class TuplesFactoryInProcess extends LocalTuplesFactory {
	
	Subject<Tuple, Tuple> tuples = ReplaySubject.create();

	@Override
	protected void publishTuple(Tuple ret) {
		tuples.onNext(ret);
	}

	@Override
	protected Observable<Tuple> query(final PrivateKey identity, Map<String, Object> criteria) {
		Observable<Tuple> t = tuples;
		for (final Entry<String, Object> criterion : criteria.entrySet()) {
			t = t.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
				String key = criterion.getKey();
				Object tupleValue = t1.get(key);
				Object value = criterion.getValue();
				if (key.equals("audience") && value == null) {
					return tupleValue == null || tupleValue.equals(identity.publicKey());
				}
				return tupleValue == null
					? value == null
					: (value != null && value.getClass().isArray() ? Arrays.equals((Object[])value, (Object[])tupleValue) : tupleValue.equals(value));
			}});
		}
		return t;
	}
	
}
