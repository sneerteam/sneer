package sneer;

import static sneer.ObservableTestUtils.*;

import org.junit.*;

public class Groups extends TestsBase {
	
	@Test
	public void targetAudience() {
		
		PrivateKey group = sneerA.createPrivateKey();
		
		cloudA.newTuplePublisher()
			.audience(group.publicKey())
			.intent("chat")
			.put("intent", "message")
			.pub("hey people!");
		
		expectValues(cloudB.newTupleSubscriber().audience(group.publicKey()).tuples(), "hey people!");
		expectValues(cloudB.newTupleSubscriber().tuples());
		assertCount(0, cloudB.newTupleSubscriber().author(userA.publicKey()).tuples());
		assertCount(0, cloudC.newTupleSubscriber().tuples());
	}
	
	@Test
	public void noLeaks() {
		
		PrivateKey group1 = sneerA.createPrivateKey();
		PrivateKey group2 = sneerA.createPrivateKey();
		
		cloudA.newTuplePublisher()
			.audience(group1.publicKey())
			.intent("chat")
			.put("intent", "message")
			.pub("hey people!");
	
		cloudA.newTuplePublisher()
			.audience(userB.publicKey())
			.intent("chat")
			.pub("hey B-dog!!");
	
		expectValues(cloudA.newTupleSubscriber().tuples());
		expectValues(cloudA.newTupleSubscriber().audience(group1.publicKey()).tuples(), "hey people!");
		expectValues(cloudA.newTupleSubscriber().audience(group2.publicKey()).tuples());
		expectValues(cloudB.newTupleSubscriber().tuples(), "hey B-dog!!");
		expectValues(cloudB.newTupleSubscriber().audience(group1.publicKey()).tuples(), "hey people!");
		expectValues(cloudB.newTupleSubscriber().audience(group2.publicKey()).tuples());
		expectValues(cloudC.newTupleSubscriber().tuples());
		expectValues(cloudC.newTupleSubscriber().audience(group1.publicKey()).tuples(), "hey people!");
		expectValues(cloudC.newTupleSubscriber().audience(group2.publicKey()).tuples());
	}
	
}
