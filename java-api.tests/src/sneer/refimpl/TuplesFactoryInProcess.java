package sneer.refimpl;

import java.util.*;
import java.util.Map.Entry;

import rx.Observable;
import rx.functions.*;
import rx.observables.*;
import rx.schedulers.*;
import rx.subjects.*;
import sneer.*;
import sneer.tuples.*;

public class TuplesFactoryInProcess {

	Subject<Tuple, Tuple> tuples = ReplaySubject.create();
	
	private static final class TupleImpl extends HashMap<String, Object> implements Tuple {
		private static final long serialVersionUID = 1L;

		public TupleImpl() {
		}
		
		@Override
		public TupleImpl clone() {
			TupleImpl t = new TupleImpl();
			t.putAll(this);
			return t;
		}

		@Override
		public PublicKey audience() {
			return (PublicKey) get("audience");
		}

		@Override
		public Object value() {
			return get("value");
		}

		@Override
		public String type() {
			return (String) get("type");
		}

		@Override
		public PublicKey author() {
			return (PublicKey) get("author");
		}

		@Override
		public String toString() {
			return "TupleImpl ["+super.toString()+"]";
		}
		
	}
	
	private final class TuplesImpl implements Tuples {
		
		
		private PrivateKey identity;

		private final class TupleSubscriberImpl implements TupleSubscriber {
			
			private Map<String, Object> where = new HashMap<String, Object>();
			
			{
				where("audience", null);
			}
			
			@Override
			public Observable<Tuple> tuples() {
				Observable<Tuple> t = tuples;
				for (final Entry<String, Object> criterion : where.entrySet()) {
					t = t.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
						String key = criterion.getKey();
						Object tupleValue = t1.get(key);
						Object value = criterion.getValue();
						if (key.equals("audience") && value == null) {
							return tupleValue == null || tupleValue.equals(identity.publicKey());
						}
						return tupleValue == null
							? value == null
							: tupleValue.equals(value);
					}});
				}
				return t;
			}

			@Override
			public TupleSubscriber type(String type) {
				where("type", type);
				return this;
			}

			@Override
			public TupleSubscriber author(PublicKey author) {
				where("author", author);
				return this;
			}

			@Override
			public TupleSubscriber audience(PrivateKey audience) {
				where("audience", audience.publicKey());
				return this;
			}

			@Override
			public Observable<Object> values() {
				return tuples().map(new Func1<Tuple, Object>() {  @Override public Object call(Tuple t1) {
					return t1.value();
				}});
			}

			@Override
			public TupleSubscriber where(String key, Object value) {
				where.put(key, value);
				return this;
			}

			@Override
			public BlockingObservable<Tuple> localTuples() {
				TestScheduler scheduler = new TestScheduler();
				final List<Tuple> result = new ArrayList<Tuple>();
				tuples().subscribeOn(scheduler).subscribe(new Action1<Tuple>() {  @Override public void call(Tuple item) {
					result.add(item);
				} });
				scheduler.triggerActions();

				return Observable.from(result).toBlockingObservable();
			}

		}

		private final class TuplePublisherImpl implements TuplePublisher {
			private TupleImpl prototype = new TupleImpl();
			
			{
				prototype.put("author", identity.publicKey());
			}

			@Override
			public void call() {
				pub();
			}

			@Override
			public void call(Object t1) {
				pub(t1);
			}

			@Override
			public TuplePublisher value(Object value) {
				prototype.put("value", value);
				return this;
			}

			@Override
			public TuplePublisher pub() {
				tuples.onNext(prototype.clone());
				return this;
			}

			@Override
			public TuplePublisher pub(Object value) {
				value(value);
				return pub();
			}

			@Override
			public TuplePublisher type(String type) {
				prototype.put("type", type);
				return this;
			}

			@Override
			public TuplePublisher audience(PublicKey audience) {
				prototype.put("audience", audience);
				return this;
			}

			@Override
			public TuplePublisher put(String key, Object value) {
				prototype.put(key, value);
				return this;
			}
		}

		public TuplesImpl(PrivateKey identity) {
			this.identity = identity;
		}

		@Override
		public TupleSubscriber newTupleSubscriber() {
			return new TupleSubscriberImpl();
		}

		@Override
		public TuplePublisher newTuplePublisher() {
			return new TuplePublisherImpl();
		}
	}

	public Tuples newTuples(PrivateKey identity) {
		return new TuplesImpl(identity);
	}

}
