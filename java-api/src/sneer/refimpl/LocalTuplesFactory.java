package sneer.refimpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.commons.Clock;
import sneer.commons.exceptions.NotImplementedYet;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.tuples.TuplePublisher;
import sneer.tuples.TupleSpace;

public abstract class LocalTuplesFactory {

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
		public Object payload() {
			return get("payload");
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

		@Override
		public long timestampCreated() {
			return (Long) get("timestampCreated");
		}

	}
	
	protected Tuple newTupleFromMap(Map<String, Object> map) {
		TupleImpl t = new TupleImpl();
		t.putAll(map);
		return t;
	}
	
	protected class TupleSpaceImpl implements TupleSpace {
		
		
		private PrivateKey identity; int isThisNecessary;

		protected final class TupleSubscriberImpl implements TupleFilter {
			
			private Map<String, Object> where = new HashMap<String, Object>();
			
			public TupleSubscriberImpl() {
			}
			
			public TupleSubscriberImpl(TupleSubscriberImpl other, String key, Object value) {
				where.putAll(other.where);
				where.put(key, value);
			}

			@Override
			public Observable<Tuple> tuples() {
				return query(identity, where);
			}

			@Override
			public TupleFilter type(String type) {
				return field("type", type);
			}

			@Override
			public TupleFilter author(PublicKey author) {
				return field("author", author);
			}

			@Override
			public TupleFilter audience(PrivateKey audience) {
				return field("audience", audience.publicKey());
			}

			@Override
			public TupleFilter field(String key, Object value) {
				return new TupleSubscriberImpl(this, key, translateValue(value));
			}

			@Override
			public Observable<Tuple> localTuples() {
				return queryLocal(identity, where);
			}

			@Override
			public TupleFilter putFields(Map<String, Object> fields) {
				TupleSubscriberImpl s = new TupleSubscriberImpl();
				s.where.putAll(fields);
				return s;
			}

			@Override
			public TupleFilter audience(PublicKey audience) {
				throw new NotImplementedYet();
			}

		}

		private final class TuplePublisherImpl implements TuplePublisher {
			private TupleImpl prototype = new TupleImpl();
			
			public TuplePublisherImpl() {
			}

			public TuplePublisherImpl(TuplePublisherImpl other) {
				prototype.putAll(other.prototype);
			}
			
			public TuplePublisherImpl(TuplePublisherImpl other, String key, Object value) {
				this(other);
				prototype.put(key, value);
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
			public Observable<Tuple> pub() {
				TupleImpl ret = prototype.clone();
				ret.put("author", identity.publicKey());
				long now = Clock.now();
				ret.put("timestampCreated", now);
				publishTuple(ret);
				return Observable.just((Tuple)ret);
			}

			@Override
			public Observable<Tuple> pub(Object value) {
				return payload(value).pub();
			}

			@Override
			public TuplePublisher payload(Object value) {
				return field("payload", value);
			}
			
			@Override
			public TuplePublisher type(String type) {
				return field("type", type);
			}

			@Override
			public TuplePublisher audience(PublicKey audience) {
				return field("audience", audience);
			}

			@Override
			public TuplePublisher field(String key, Object value) {
				return new TuplePublisherImpl(this, key, translateValue(value));
			}

			@Override
			public TuplePublisher putFields(Map<String, Object> fields) {
				TuplePublisherImpl t = new TuplePublisherImpl();
				t.prototype.putAll(fields);
				return t;
			}

		}

		public TupleSpaceImpl(PrivateKey prik) {
			identity = prik;
		}

		@Override
		public TupleFilter filter() {
			return new TupleSubscriberImpl();
		}

		@Override
		public TuplePublisher publisher() {
			return new TuplePublisherImpl();
		}
	}

	abstract protected void publishTuple(Tuple ret);
	
	abstract protected Observable<Tuple> query(PrivateKey identity, Map<String, Object> criteria);
	
	abstract protected Observable<Tuple> queryLocal(PrivateKey identity, Map<String, Object> criteria);
	
	public TupleSpace newTupleSpace(PrivateKey prik) {
		return new TupleSpaceImpl(prik);
	}

	private static Object translateValue(final Object value) {
		if (value != null) {
			if (value.getClass().isArray()) {
				Object[] array = (Object[]) value;
				return Arrays.asList(array);
			} else if (value instanceof Number) {
				return Long.valueOf(value.toString());
			}
		}
		return value;
	}

}
