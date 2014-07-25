package sneer.refimpl;

import java.util.*;
import java.util.Map.Entry;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.functions.*;
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
	}
	
	private final class TuplesImpl implements TupleSpace {
		
		
		private PrivateKey identity;

		private final class TupleSubscriberImpl implements TupleFilter {
			
			private Map<String, Object> where = new HashMap<String, Object>();
			
			public TupleSubscriberImpl() {
				where.put("audience", null);
			}
			
			public TupleSubscriberImpl(TupleSubscriberImpl other, String key, Object value) {
				where.putAll(other.where);
				where.put(key, value);
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
							: (value.getClass().isArray() ? Arrays.equals((Object[])value, (Object[])tupleValue) : tupleValue.equals(value));
					}});
				}
				return t;
			}

			@Override
			public TupleFilter byType(String type) {
				return byField("type", type);
			}

			@Override
			public TupleFilter byAuthor(PublicKey author) {
				return byField("author", author);
			}

			@Override
			public TupleFilter byAudience(PrivateKey audience) {
				return byField("audience", audience.publicKey());
			}

			@Override
			public TupleFilter byField(String key, Object value) {
				return new TupleSubscriberImpl(this, key, value);
			}

			@Override
			public Observable<Tuple> localTuples() {
				return Observable.create(new OnSubscribe<Tuple>() { @Override public void call(final Subscriber<? super Tuple> t1) {
					TestScheduler scheduler = new TestScheduler();
					tuples().subscribeOn(scheduler).subscribe(new Action1<Tuple>() {  @Override public void call(Tuple item) {
						t1.onNext(item);
					} });
					scheduler.triggerActions();
					t1.onCompleted();
				}});
			}

		}

		private final class TuplePublisherImpl implements TuplePublisher {
			private TupleImpl prototype = new TupleImpl();
			
			{
				prototype.put("author", identity.publicKey());
			}

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
			public Tuple pub() {
				TupleImpl ret = prototype.clone();
				tuples.onNext(ret);
				return ret;
			}

			@Override
			public Tuple pub(Object value) {
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
				return new TuplePublisherImpl(this, key, value);
			}
		}

		public TuplesImpl(PrivateKey identity) {
			this.identity = identity;
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

	public TupleSpace newTuples(PrivateKey identity) {
		return new TuplesImpl(identity);
	}

}
