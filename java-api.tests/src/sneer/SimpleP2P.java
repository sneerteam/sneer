package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.tuples.TupleUtils.*;

import java.io.*;

import org.junit.*;

import sneer.impl.keys.*;
import sneer.tuples.*;

public class SimpleP2P extends TestsBase {
	
	
	@Test
	public void messagePassing() throws IOException {

		TuplePublisher publisher = tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");
			
		publisher.pub("rock");
		
		publisher.type("rock-paper-scissor/message")
			.pub("hehehe");
		
		
		TupleSubscriber subscriber = tuplesB.newTupleSubscriber();

		expectValues(subscriber.tuples(), "paper", "rock", "hehehe");
		expectValues(subscriber.type("rock-paper-scissor/move").tuples(), "paper", "rock");
		expectValues(subscriber.type("rock-paper-scissor/message").tuples(), "hehehe");
		
	}

	@Test
	public void tupleWithType() throws IOException {

		tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper")
			.type("rock-paper-scissor/message")
			.pub("hehehe");
		
		assertEqualsUntilNow(tuplesB.newTupleSubscriber().tuples().map(TO_TYPE), "rock-paper-scissor/move", "rock-paper-scissor/message");
		
	}
	
	@Test
	public void targetUser() {
		
		tuplesA.newTuplePublisher()
			.audience(userC.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");
		
		assertCount(0, tuplesB.newTupleSubscriber().tuples());
		assertCount(1, tuplesC.newTupleSubscriber().tuples());
	}
	
	@Test
	public void publicTuples() {
		
		tuplesA.newTuplePublisher()
			.type("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesA.newTupleSubscriber().tuples()); // should I receive my own public tuples?
		assertCount(1, tuplesB.newTupleSubscriber().tuples());
		assertCount(1, tuplesC.newTupleSubscriber().tuples());
		
	}
	
	@Test
	public void byAuthor() {
		tuplesA.newTuplePublisher()
			.type("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesB.newTupleSubscriber().author(userA.publicKey()).tuples());
		assertCount(0, tuplesB.newTupleSubscriber().author(userC.publicKey()).tuples());
	}
	
	@Test
	public void audienceIgnoresPublic() {
		
		tuplesA.newTuplePublisher()
			.type("chat/message")
			.pub("hey people!");
		
		PrivateKey group = Keys.createPrivateKey();
		assertCount(0, tuplesB.newTupleSubscriber().audience(group).tuples());
	}
	
}
