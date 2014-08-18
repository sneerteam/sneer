package sneer.core.tests;

import static org.junit.Assert.*;
import static sneer.core.tests.ObservableTestUtils.*;
import static sneer.core.tests.TupleTestUtils.*;
import static sneer.tuples.Tuple.*;

import org.junit.*;

import rx.Observable;
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
		TupleSpace tuplesD = newTupleSpace(userD, newPeers(userA));
		
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
			.type("profile/name");
		publisher.pub("old name");
		publisher.pub("new name");

		
		assertEquals(
			"new name",
			tuplesA.filter()
				.audience(userA)
				.type("profile/name")
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

}
