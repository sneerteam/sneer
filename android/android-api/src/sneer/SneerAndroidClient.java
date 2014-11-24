package sneer;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.ReplaySubject;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.tuples.TupleSpace;
import sneer.utils.Value;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;

public class SneerAndroidClient {

	public static final String TYPE = "type";
	public static final String PARTY_PUK = "partyPuk";

	public static final String TITLE = "title";
	public static final String DISABLE_MENUS = "disable-menus";

	//Message
	public static final String TEXT = "text";
	public static final String JPEG_IMAGE = "jpeg-image";
	public static final String PAYLOAD = "payload";
	public static final String RESULT_RECEIVER = "result";

	//Session
	public static final String PARTNER_NAME = "partnerName";
	public static final String OWN = "own";
	public static final String REPLAY_FINISHED = "replayFinished";
	public static final String ERROR = "error";
	public static final String UNSUBSCRIBE = "unsubscribe";

	@Deprecated
	public static final String SESSION_ID = "sessionId";
	@Deprecated
	public static final String OWN_PRIK = "ownPrik";

//	private static final PrivateKey EMPTY_KEY = new PrivateKey() {
//		private static final long serialVersionUID = 1L;
//
//		@Override public PublicKey publicKey() {
//			return null;
//		}
//
//		@Override
//		public byte[] toBytes() {
//			return null;
//		}
//
//		@Override
//		public String toHex() {
//			return null;
//		}
//	};

//	private final Context context;
	private TupleSpace tupleSpace;


	static Object unbundle(Bundle resultData) {
		return resultData.get("value");
	}


	public SneerAndroidClient(Context context) {
//		this.context = context;
	}


	static class SessionInfo {
		long id;
		PublicKey partyPuk;
		String type;
		long lastMessageSeen;
		public SessionInfo(long id, PublicKey partyPuk, String type, long lastMessageSeen) {
			this.id = id;
			this.partyPuk = partyPuk;
			this.type = type;
			this.lastMessageSeen = lastMessageSeen;
		}
	}


	public static Observable<String> partyName(TupleSpace tupleSpace, PublicKey partyPuk, PrivateKey ownPrik) {
		return tupleSpace.filter()
			.audience(ownPrik)
			.type("contact")
			.field("party", partyPuk)
			.tuples()
			.map(Tuple.TO_PAYLOAD)
			.cast(String.class);
	};


	public TupleSpace tupleSpace() {
//		if (tupleSpace == null)
//			tupleSpace = new TupleSpaceFactoryClient(context).newTupleSpace(EMPTY_KEY);
		return tupleSpace;
	}


	public Session session(final long id, final PrivateKey ownPrik) {
		final ReplaySubject<SessionInfo> sessionInfo = ReplaySubject.create();

		tupleSpace().filter()
			.audience(ownPrik)
			.type("sneer/session")
			.field("session", id)
			.localTuples()
			.last()
			.map(new Func1<Tuple, SessionInfo>() { @Override public SessionInfo call(Tuple tuple) {
				return new SessionInfo(id, (PublicKey)tuple.get("partyPuk"), (String)tuple.get("sessionType"), (Long)tuple.get("lastMessageSeen"));
			}})
			.subscribe(sessionInfo);

		return new Session() {
			@Override
			public void sendMessage(final Object content) {
				sessionInfo.subscribe(new Action1<SessionInfo>() { @Override public void call(SessionInfo info) {
					tupleSpace().publisher()
						.audience(info.partyPuk)
						.type(info.type)
						.field("session", info.id)
						.pub(content);
				}});
			}

			@Override
			public void dispose() {
				// TODO
			}

			@Override
			public Observable<String> peerName() {
				return Observable.create(new OnSubscribe<String>() { @Override public void call(final Subscriber<? super String> subscriber) {
					sessionInfo.subscribe(new Action1<SessionInfo>() { @Override public void call(SessionInfo session) {
						subscriber.add(partyName(tupleSpace(), session.partyPuk, ownPrik).subscribe(subscriber));
					}});
				}});
			}

			@Override
			public Observable<Message> previousMessages() {
				return messages(new Func2<SneerAndroidClient.SessionInfo, Message, Boolean>() { @Override public Boolean call(SessionInfo session, Message msg) {
					return msg.timestampCreated() <= session.lastMessageSeen;
				}}, new Func1<TupleFilter, Observable<Tuple>>() { @Override public Observable<Tuple> call(TupleFilter filter) {
					return filter.localTuples();
				}});
			}

			@Override
			public Observable<Message> newMessages() {
				return messages(new Func2<SneerAndroidClient.SessionInfo, Message, Boolean>() { @Override public Boolean call(SessionInfo session, Message msg) {
					return msg.timestampCreated() > session.lastMessageSeen;
				}}, new Func1<TupleFilter, Observable<Tuple>>() { @Override public Observable<Tuple> call(TupleFilter filter) {
					return filter.tuples();
				}});
			}

			private Observable<Message> messages(final Func2<SessionInfo, Message, Boolean> predicate, final Func1<TupleFilter, Observable<Tuple>> tuples) {
				return Observable.create(new OnSubscribe<Message>() { @Override public void call(final Subscriber<? super Message> subscriber) {
					sessionInfo.subscribe(new Action1<SessionInfo>() { @Override public void call(final SessionInfo session) {
						Subscription subscription =
							tuples.call(tupleSpace()
								.filter()
								.type(session.type)
								.field("session", session.id))
							.map(MessageImpl.fromTuple(ownPrik.publicKey()))
							.filter(new Func1<Message, Boolean>() { @Override public Boolean call(Message msg) {
								return predicate.call(session, msg);
							}})
						.subscribe(subscriber);
						subscriber.add(subscription);
					}});
				}});
			}
		};
	}


	public static void send(ResultReceiver resultReceiver, String label, Object payload, byte[] jpegImage) {
		Bundle bundle = new Bundle();
		bundle.putString(TEXT, label);
		bundle.putByteArray(JPEG_IMAGE, jpegImage);
		bundle.putParcelable(PAYLOAD, Value.of(payload));
		resultReceiver.send(Activity.RESULT_OK, bundle);
	}

}
