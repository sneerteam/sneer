package core;

import static core.ObservableTestUtils.*;
import static core.TupleTestUtils.*;
import static org.junit.Assert.*;
import static sneer.tuples.Tuple.*;

import java.io.*;

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
	public void messagePassing() throws IOException {

		TuplePublisher publisher = tuplesA.publisher()
			.audience(userB.publicKey())
			.type("rock-paper-scissor/move");
			
		publisher.pub("paper");
		publisher.pub("rock");
		
		publisher.type("rock-paper-scissor/message")
			.pub("hehehe");
		
		
		TupleFilter subscriber = tuplesB.filter();

		expectValues(subscriber.tuples(), "paper", "rock", "hehehe");
		expectValues(subscriber.type("rock-paper-scissor/move").tuples(), "paper", "rock");
		expectValues(subscriber.type("rock-paper-scissor/message").tuples(), "hehehe");
		
	}

	@Test
	public void tupleWithType() throws IOException {

		TuplePublisher publisher = tuplesA.publisher()
			.audience(userB.publicKey());
		publisher.type("rock-paper-scissor/move").pub("paper");
		publisher.type("rock-paper-scissor/message").pub("hehehe");
		
		assertEqualsUntilNow(tuplesB.filter().tuples().map(TO_TYPE), "rock-paper-scissor/move", "rock-paper-scissor/message");
		
	}
	
	@Test
	public void targetUser() {
		
		tuplesA.publisher()
			.audience(userC.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");
		
		assertCount(0, tuplesB.filter().tuples());
		assertCount(1, tuplesC.filter().tuples());
	}
	
	@Test
	public void publicTuples() {
		
		tuplesA.publisher()
			.type("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesB.filter().tuples());
		assertCount(1, tuplesC.filter().tuples());
		assertCount(1, tuplesA.filter().tuples()); // should I receive my own public tuples?
		
	}
	
	@Test
	public void byAuthor() {
		tuplesA.publisher()
			.type("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesB.filter().author(userA.publicKey()).tuples());
		assertCount(0, tuplesB.filter().author(userC.publicKey()).tuples());
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
