package sneer;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.functions.*;
import rx.subjects.*;
import sneer.tuples.*;
import sneer.utils.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class SneerAndroid {

// This is how one uses the startConversationList() method to start Sneer with conversations filtered by tuple type:
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		
//		String title = "RPS Challenges";
//		String type = "rock-paper-scissors/move";
//		String newConversationLabel = "Challenge!!";
//		String newConversationAction = "sneer.tutorial.rockpaperscissors.CHALLENGE";
//		
//		SneerAndroid.startConversationList(this, title, type, newConversationLabel, newConversationAction);
//	}

	
	public static final String TYPE = "type";
	public static final String PARTY_PUK = "partyPuk";
	
	private static final String CONVERSATION_LIST = "sneer.android.main.ui.MAIN";
	public static final String TITLE = "title";
	public static final String NEW_CONVERSATION_LABEL = "newConversationLabel";
	public static final String NEW_CONVERSATION_ACTION = "newConversationAction";
	public static final String DISABLE_MENUS = "disable-menus";
	static final String SNEER_SERVICE = "sneer.android.service.BACKEND";

	public static final String SESSION_ID = "sessionId";
	public static final String OWN_PRIK = "ownPrik";

	
	public static void startMain(Activity activity, String title, String type, String newConversationLabel, String newConversationAction) {
		try {
			Intent intent = new Intent(CONVERSATION_LIST);
			intent.putExtra(TITLE, title);
			intent.putExtra(TYPE, type);
			intent.putExtra(NEW_CONVERSATION_LABEL, newConversationLabel);
			intent.putExtra(NEW_CONVERSATION_ACTION, newConversationAction);
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			SneerUtils.showInstallSneerDialog(activity);
		}
	}
	
	private static final PrivateKey EMPTY_KEY = new PrivateKey() {
		private static final long serialVersionUID = 1L;

		@Override public PublicKey publicKey() {
			return null;
		}

		@Override
		public byte[] bytes() {
			return null;
		}

		@Override
		public String bytesAsString() {
			return null;
		}
	};
	
	private Context context;
	private TupleSpace tupleSpace;

	static Object unbundle(Bundle resultData) {
		return resultData.get("value");
	}
	
	public SneerAndroid(Context context) {
		this.context = context;
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
			.type("sneer/contact")
			.field("party", partyPuk)
			.tuples()
			.map(Tuple.TO_PAYLOAD)
			.cast(String.class);
	};
	
	public TupleSpace tupleSpace() {
		if (tupleSpace == null) {
			tupleSpace = new TupleSpaceFactoryClient(context).newTupleSpace(EMPTY_KEY);
		}
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
			.map(new Func1<Tuple, SessionInfo>() {  @Override public SessionInfo call(Tuple t1) {
				return new SessionInfo(id, (PublicKey)t1.get("partyPuk"), (String)t1.get("sessionType"), (Long)t1.get("lastMessageSeen"));
			} })
			.subscribe(sessionInfo);

		
		return new Session() {

			@Override
			public void sendMessage(final Object content) {
				sessionInfo.subscribe(new Action1<SessionInfo>() {  @Override public void call(SessionInfo t1) {
					tupleSpace().publisher()
						.audience(t1.partyPuk)
						.type(t1.type)
						.field("session", t1.id)
						.pub(content);
				} });
			}

			@Override
			public void dispose() {
				// TODO
			}
			
			@Override
			public Observable<String> peerName() {
				return Observable.create(new OnSubscribe<String>() {  @Override public void call(final Subscriber<? super String> subscriber) {
					sessionInfo.subscribe(new Action1<SessionInfo>() {  @Override public void call(SessionInfo session) {
						subscriber.add(partyName(tupleSpace(), session.partyPuk, ownPrik).subscribe(subscriber));
					}});
				}});
			}
			
			@Override
			public Observable<Message> previousMessages() {
				return messages(new Func2<SneerAndroid.SessionInfo, Message, Boolean>() {  @Override public Boolean call(SessionInfo session, Message msg) {
					return msg.timestampReceived() <= session.lastMessageSeen;
				} }, new Func1<TupleFilter, Observable<Tuple>>() {  @Override public Observable<Tuple> call(TupleFilter t1) {
					return t1.localTuples();
				} });
			}
			
			@Override
			public Observable<Message> newMessages() {
				return messages(new Func2<SneerAndroid.SessionInfo, Message, Boolean>() {  @Override public Boolean call(SessionInfo session, Message msg) {
					return msg.timestampReceived() > session.lastMessageSeen;
				} }, new Func1<TupleFilter, Observable<Tuple>>() {  @Override public Observable<Tuple> call(TupleFilter t1) {
					return t1.tuples();
				} });
			}
			
			private Observable<Message> messages(final Func2<SessionInfo, Message, Boolean> predicate, final Func1<TupleFilter, Observable<Tuple>> tuples) {
				return Observable.create(new OnSubscribe<Message>() {  @Override public void call(final Subscriber<? super Message> subscriber) {
					sessionInfo.subscribe(new Action1<SessionInfo>() {  @Override public void call(final SessionInfo session) {
						
						Subscription subscription = 
							tuples.call(tupleSpace()
								.filter()
								.type(session.type)
								.field("session", session.id))
							.map(Message.fromTuple(ownPrik.publicKey()))
							.filter(new Func1<Message, Boolean>() {  @Override public Boolean call(Message msg) {
								return predicate.call(session, msg);
							}})
						.subscribe(subscriber);
						
						subscriber.add(subscription);
					}});
				}});
			}
			
		};
	}

}
