package sneer.core.tests;

import static org.junit.Assert.*;
import static sneer.core.tests.ObservableTestUtils.*;
import static sneer.core.tests.TupleTestUtils.*;
import static sneer.tuples.Tuple.*;

import org.junit.*;

import rx.*;
import sneer.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class SimpleP2P extends TupleSpaceTestsBase {
	
	@Test
	public void publisherFluentReturningNewInstance() {
		assertNotSame(tuplesA.publisher(), tuplesA.publisher());
		TuplePublisher publisher = tuplesA.publisher();
		assertNotSame(publisher, publisher.audience(userA.publicKey()));
		assertNotSame(publisher, publisher.pub());
	}
	
	@Test
	public void subscriberFluentReturningNewInstance() {
		assertNotSame(tuplesA.filter(), tuplesA.filter());
		TupleFilter subscriber = tuplesA.filter();
		assertNotSame(subscriber, subscriber.audience(userA));
		assertNotSame(subscriber, subscriber.type("bla"));
	}
	
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
			.pub("UserA McCloud");
		tuplesB.publisher()
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
		
		PrivateKey neide = Keys.createPrivateKey();
		assertCount(0, tuplesB.filter().audience(neide).tuples());
	}
	
	@Test
	public void completedLocalTuples() {
		
		TuplePublisher publisher = tuplesA.publisher()
			.audience(userA.publicKey())
			.type("profile/name")
			.field("custom", 42);
			
		publisher.pub("old name");
		publisher.pub("new name");

		
		assertEquals(
			"new name",
			tuplesA.filter()
				.audience(userA)
				.type("profile/name")
				.field("custom", 42)
				.localTuples().toBlocking().last().payload());
	}
	
	@Test
	public void subscriberCriteriaWithArray() {
		Object[] array = {"notes", userB.publicKey()};
		
		tuplesA.publisher()
			.audience(userA.publicKey())
			.type("file")
			.field("path", array)
			.pub("userB is cool");
			
		Observable<Tuple> actual = tuplesA.filter()
				.audience(userA)
				.type("file")
				.field("path", array)
				.tuples();
		
		expectValues(actual, "userB is cool");
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
