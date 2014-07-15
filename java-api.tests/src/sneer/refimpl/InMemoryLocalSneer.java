package sneer.refimpl;

import java.util.*;
import java.util.Map.Entry;

import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;

public class InMemoryLocalSneer implements Sneer {

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
		public String intent() {
			return (String) get("intent");
		}

		@Override
		public PublicKey author() {
			return (PublicKey) get("author");
		}

		@Override
		public String toString() {
			return "TupleImpl ["+this+"]";
		}
		
	}
	
	private final class CloudImpl implements Cloud {
		
		
		private KeyPair identity;

		private final class TupleSubscriberImpl implements TupleSubscriber {
			
			private Map<String, Object> where = new HashMap<String, Object>();
			
			{
				where.put("audience", identity);
			}

			@Override
			public Observable<Tuple> tuples() {
				Observable<Tuple> t = tuples;
				for (final Entry<String, Object> entry : where.entrySet()) {
					t = t.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
						return t1.get(entry.getKey()).equals(entry.getValue());
					}});
				}
				return t;
			}

			@Override
			public TupleSubscriber intent(String intent) {
				where("intent", intent);
				return this;
			}

			@Override
			public TupleSubscriber author(PublicKey author) {
				where("author", author);
				return this;
			}

			@Override
			public TupleSubscriber audience(PrivateKey audience) {
				where("audience", audience);
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

		}

		private final class TuplePublisherImpl implements TuplePublisher {
			private TupleImpl prototype = new TupleImpl();

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
			public TuplePublisher intent(String... intent) {
				prototype.put("intent", intent);
				return this;
			}

			@Override
			public TuplePublisher audience(PublicKey audience) {
				prototype.put("audience", audience);
				return this;
			}

			@Override
			public TuplePublisher put(String key, Object value) {
				return this;
			}
		}

		public CloudImpl(KeyPair identity) {
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

	private static final class KeyPairImpl implements KeyPair {
	}
	

	@Override
	public KeyPair newKeyPair() {
		return new KeyPairImpl();
	}

	@Override
	public Cloud newCloud(KeyPair identity) {
		return new CloudImpl(identity);
	}

}
