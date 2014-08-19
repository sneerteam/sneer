package sneer.refimpl;

import java.util.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.functions.*;
import rx.schedulers.*;
import sneer.*;
import sneer.commons.exceptions.*;
import sneer.tuples.*;

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
				where.put("audience", null);
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
			public TupleFilter audienceMe() {
				throw new NotImplementedYet();
			}
			
			@Override
			public TupleFilter audience(PrivateKey audience) {
				return field("audience", audience.publicKey());
			}

			@Override
			public TupleFilter field(String key, Object value) {
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

			@Override
			public TupleFilter putFields(Map<String, Object> fields) {
				TupleSubscriberImpl s = new TupleSubscriberImpl();
				s.where.putAll(fields);
				return s;
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
				publishTuple(ret);
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
	
	public TupleSpace newTupleSpace(PrivateKey prik) {
		return new TupleSpaceImpl(prik);
	}

}
