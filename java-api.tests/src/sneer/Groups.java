package sneer;

import static sneer.ObservableTestUtils.*;

import org.junit.*;

import sneer.impl.keys.*;

public class Groups extends TestsBase {
	
	@Test
	public void targetAudience() {
		
		PrivateKey group = Keys.createPrivateKey();
		
		tuplesA.publisher()
			.audience(group.publicKey())
			.type("chat")
			.field("type", "message")
			.pub("hey people!");
		
		expectValues(tuplesB.filter().byAudience(group).tuples(), "hey people!");
		expectValues(tuplesB.filter().tuples());
		assertCount(0, tuplesB.filter().byAuthor(userA.publicKey()).tuples());
		assertCount(0, tuplesC.filter().tuples());
	}
	
	@Test
	public void noLeaks() {
		
		PrivateKey group1 = Keys.createPrivateKey();
		PrivateKey group2 = Keys.createPrivateKey();
		
		tuplesA.publisher()
			.audience(group1.publicKey())
			.type("chat")
			.field("type", "message")
			.pub("hey people!");
	
		tuplesA.publisher()
			.audience(userB.publicKey())
			.type("chat")
			.pub("hey B-dog!!");
	
		expectValues(tuplesA.filter().tuples());
		expectValues(tuplesA.filter().byAudience(group1).tuples(), "hey people!");
		expectValues(tuplesA.filter().byAudience(group2).tuples());
		expectValues(tuplesB.filter().tuples(), "hey B-dog!!");
		expectValues(tuplesB.filter().byAudience(group1).tuples(), "hey people!");
		expectValues(tuplesB.filter().byAudience(group2).tuples());
		expectValues(tuplesC.filter().tuples());
		expectValues(tuplesC.filter().byAudience(group1).tuples(), "hey people!");
		expectValues(tuplesC.filter().byAudience(group2).tuples());
	}
	
}
