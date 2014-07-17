package sneer;

import static sneer.ObservableTestUtils.*;

import org.junit.*;

import sneer.impl.*;

public class Groups extends TestsBase {
	
	@Test
	public void targetAudience() {
		
		PrivateKey group = Keys.newPrivateKey();
		
		tuplesA.newTuplePublisher()
			.audience(group.publicKey())
			.intent("chat")
			.put("intent", "message")
			.pub("hey people!");
		
		expectValues(tuplesB.newTupleSubscriber().audience(group.publicKey()).tuples(), "hey people!");
		expectValues(tuplesB.newTupleSubscriber().tuples());
		assertCount(0, tuplesB.newTupleSubscriber().author(userA.publicKey()).tuples());
		assertCount(0, tuplesC.newTupleSubscriber().tuples());
	}
	
	@Test
	public void noLeaks() {
		
		PrivateKey group1 = Keys.newPrivateKey();
		PrivateKey group2 = Keys.newPrivateKey();
		
		tuplesA.newTuplePublisher()
			.audience(group1.publicKey())
			.intent("chat")
			.put("intent", "message")
			.pub("hey people!");
	
		tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.intent("chat")
			.pub("hey B-dog!!");
	
		expectValues(tuplesA.newTupleSubscriber().tuples());
		expectValues(tuplesA.newTupleSubscriber().audience(group1.publicKey()).tuples(), "hey people!");
		expectValues(tuplesA.newTupleSubscriber().audience(group2.publicKey()).tuples());
		expectValues(tuplesB.newTupleSubscriber().tuples(), "hey B-dog!!");
		expectValues(tuplesB.newTupleSubscriber().audience(group1.publicKey()).tuples(), "hey people!");
		expectValues(tuplesB.newTupleSubscriber().audience(group2.publicKey()).tuples());
		expectValues(tuplesC.newTupleSubscriber().tuples());
		expectValues(tuplesC.newTupleSubscriber().audience(group1.publicKey()).tuples(), "hey people!");
		expectValues(tuplesC.newTupleSubscriber().audience(group2.publicKey()).tuples());
	}
	
}
