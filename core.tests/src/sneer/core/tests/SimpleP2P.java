package sneer.core.tests;

import static org.junit.Assert.*;
import static sneer.core.tests.ObservableTestUtils.*;
import static sneer.tuples.Tuple.*;

import org.junit.*;

import rx.*;
import rx.functions.*;
import sneer.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class SimpleP2P extends TupleSpaceTestsBase {
	
	@Test
	public void messagePassing() {
		
		TuplePublisher publisher = tuplesA.publisher()
			.audience(userB.publicKey())
			.type("rock-paper-scissor/move");
			
		publisher.pub("paper");
		publisher.pub("rock");
		
		publisher.type("rock-paper-scissor/message")
			.pub("hehehe");
		
		TupleFilter subscriber = tuplesB.filter();		
		expecting(
			payloads(subscriber.tuples(), "paper", "rock", "hehehe"),
			payloads(subscriber.type("rock-paper-scissor/move").tuples(), "paper", "rock"),
			payloads(subscriber.type("rock-paper-scissor/message").tuples(), "hehehe"));
		
	}

	@Test
	public void tupleWithType() {

		TuplePublisher publisher = tuplesA.publisher()
			.audience(userB.publicKey());
		publisher.type("rock-paper-scissor/move").pub("paper");
		publisher.type("rock-paper-scissor/message").pub("hehehe");
		
		expecting(
			values(tuplesB.filter().tuples().map(TO_TYPE), "rock-paper-scissor/move", "rock-paper-scissor/message"));
		
	}
	
	@Test
	public void targetUser() {
		
		tuplesA.publisher()
			.audience(userC.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");
		
		tuplesA.publisher()
			.type("sentinel")
			.pub("end");
		
		expecting(
			payloads(tuplesB.filter().tuples(), "end"),
			payloads(tuplesC.filter().tuples(), "paper", "end"));
	}
	
	@Test
	public void publicTuples() {
		
		String name = "UserA McCloud";
		tuplesA.publisher()
			.type("profile/name")
			.pub(name);
		
		expecting(
			payloads(tuplesB.filter().tuples(), name),
			payloads(tuplesC.filter().tuples(), name),
			payloads(tuplesA.filter().tuples(), name));
	}
	
	@Test
	public void newPeerCanSubscribeToPastTuples() {
		
		String name = "UserA McCloud";
		tuplesA.publisher()
			.type("profile/name")
			.pub(name);
		
		PrivateKey userD = Keys.createPrivateKey();
		TupleSpace tuplesD = newTupleSpace(userD, followees(userA));
		
		expecting(
			payloads(tuplesD.filter().tuples(), name));
	}
	
	@Test
	public void byAuthor() {
		tuplesA.publisher()
			.type("user/name")
			.pub("UserA McCloud");
		tuplesB.publisher()
			.type("user/name")
			.pub("UserB McCloud");
		
		expecting(
			payloads(tuplesC.filter().author(userA.publicKey()).tuples(), "UserA McCloud"),
			payloads(tuplesC.filter().author(userB.publicKey()).tuples(), "UserB McCloud"));
	}
	
	@Test
	public void audienceIgnoresPublic() {
		
		tuplesA.publisher()
			.type("chat/message")
			.pub("hey people!");
		
		tuplesA.publisher()
			.audience(userB.publicKey())
			.type("sentinel")
			.pub("eof");
		
		expecting(
			payloads(tuplesB.filter().audience(userB).tuples(), "eof"));
	}
	
	@Test
	public void customField() {
		TuplePublisher customPublisher = tuplesA.publisher().type("custom");
		customPublisher.field("custom", 42).pub();
		customPublisher.field("custom", 23).pub();
		
		expecting(
			values(
				tuplesA
					.filter()
					.field("custom", 42)
					.tuples()
					.map(field("custom")),
				42));
	}

	@Test
	public void completedLocalTuples() {
		
		TuplePublisher publisher = tuplesA.publisher()
			.audience(userA.publicKey())
			.type("profile/name");
			
		publisher.pub("old name");
		publisher.pub("new name");
		
		expecting(
			notifications(
				tuplesA.filter()
					.audience(userA)
					.type("profile/name")
					.localTuples()
					.map(Tuple.TO_PAYLOAD),
				Notification.createOnNext("old name"),
				Notification.createOnNext("new name"),
				Notification.createOnCompleted()));
	}
	
	@Test
	public void localTupleQueriesAreDeferredUntilSubscribe() {
		
		TuplePublisher publisher = tuplesA.publisher().type("local");			
		publisher.pub("before");
		
		Observable<Object> localPayloads = tuplesA.filter()
				.type("local")
				.localTuples()
				.map(Tuple.TO_PAYLOAD);
		
		publisher.pub("after");
		
		expecting(
			notifications(
				localPayloads,
				Notification.createOnNext("before"),
				Notification.createOnNext("after"),
				Notification.createOnCompleted()));
		
		publisher.pub("later");
		
		expecting(
			notifications(
				localPayloads,
				Notification.createOnNext("before"),
				Notification.createOnNext("after"),
				Notification.createOnNext("later"),
				Notification.createOnCompleted()));
	}
	
	@Test
	public void subscriberCriteriaWithArray() {
		final Object[] array = {"notes", userB.publicKey()};
		
		tuplesA.publisher()
			.audience(userA.publicKey())
			.type("file")
			.field("path", array)
			.pub("userB is cool");
			
		Observable<Tuple> actual = tuplesA.filter()
				.audience(userA)
				.type("file")
				//.field("path", array)
				.tuples();
		
		expecting(
			actual.map(new Func1<Tuple, Void>() {  @Override public Void call(Tuple t1) {
				assertArrayEquals(array, (Object[])t1.get("path"));				
				assertEquals("userB is cool", t1.payload());
				return null;
			}}));
	}
	
//	@Test
//	public void pubIfEmpty() {
//		
//		TuplePublisher publisher = tuplesA.publisher()
//			.audience(userA.publicKey())
//			.type("sneer/profile.nickname");
//		
//		TupleFilter filter = tuplesA.filter()
//			.audience(userA)
//			.type("profile/profile.nickname");
//		
//		expecting(
//				values(publisher.pubIfEmpty(filter, "a"), Pair.of(true, "a")),
//				values(publisher.pubIfEmpty(filter, "b"), Pair.of(false, "a")));
//		
//	}
//	
//	interface PartyId {
//		
//	}
//
//	@Test
//	public void pubIfEmptay() {
//		
//		tuplesA.filter()
//			.author(userA.publicKey())
//			.type("sneer/party")
//			.localTuples()
//			.flatMap(new Func1<Tuple, Observable<Tuple>>() {  @Override public Observable<Tuple> call(Tuple contact) {
//				return tuplesA.filter()
//						.author(userA.publicKey())
//						.type("sneer/contact")
//						.field("partyId", contact.get("partyId"))
//						.localTuples()
//						.last();
//			} })
//			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
//				return !t1.containsKey("deleted");
//			} })
//			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
//				
//				System.out.println("party id: " + t1.get("partyId") +  ", nickname: " + t1.payload());
//			} });
//		
//		
//		Observable<String> allNicknames = tuplesA.filter()
//			.author(userA.publicKey())
//			.type("sneer/contact")
//			.localTuples()
//			.groupBy(new Func1<Tuple, PartyId>() {  @Override public PartyId call(Tuple tuple) {
//				return (PartyId) tuple.get("partyId");
//			} })
//			.flatMap(new Func1<GroupedObservable<PartyId, Tuple>, Observable<Tuple>>() {  @Override public Observable<Tuple> call(GroupedObservable<PartyId, Tuple> group) {
//				return group.last();
//			} })
//			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
//				return !t1.containsKey("deleted");
//			} })
//			.map(new Func1<Tuple, Object>() {  @Override public Object call(Tuple t1) {
//				return t1.get("nickname");
//			} })
//			.cast(String.class);
//		
//		allNicknames
//			.filter(new Func1<String, Boolean>() {  @Override public Boolean call(String t1) {
//				return t1.equals("new nickname");
//			} });
//			
//			
////			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
////				System.out.println("party id: " + t1.get("partyId") +  ", last-known-puk: " + t1.get("puk") + ", nickname: " + t1.get("nickname"));
////			} });
//
//		
//		tuplesA.filter()
//			.author(userA.publicKey())
//			.type("sneer/party")
//			.localTuples()
//			.groupBy(new Func1<Tuple, PartyId>() {  @Override public PartyId call(Tuple tuple) {
//				return (PartyId) tuple.get("partyId");
//			}})
//			.flatMap(new Func1<GroupedObservable<PartyId, Tuple>, Observable<Tuple>>() {  @Override public Observable<Tuple> call(GroupedObservable<PartyId, Tuple> group) {
//				return group.last();
//			} })
//			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
//				System.out.println("party id: " + t1.get("partyId") +  ", last-known-puk: " + t1.get("puk") + ", nickname: " + t1.get("nickname"));
//			} });
//
//	}

}
