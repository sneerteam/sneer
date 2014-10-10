package sneer.core.tests;

import static sneer.core.tests.ObservableTestUtils.assertCount;
import static sneer.core.tests.TupleTestUtils.expectValues;

import org.junit.Ignore;
import org.junit.Test;

import sneer.PrivateKey;
import sneer.impl.keys.KeysImpl;

@Ignore("wip")
public class Groups extends TupleSpaceTestsBase {
	
	@Test
	public void targetAudience() {
		
		PrivateKey group = new KeysImpl().createPrivateKey();
		
		tuplesA.publisher()
			.audience(group.publicKey())
			.type("chat")
			.field("type", "message")
			.pub("hey people!");
		
		expectValues(tuplesB.filter().audience(group).tuples(), "hey people!");
		expectValues(tuplesB.filter().tuples());
		assertCount(0, tuplesB.filter().author(userA.publicKey()).tuples());
		assertCount(0, tuplesC.filter().tuples());
	}
	
	@Test
	public void noLeaks() {
		
		PrivateKey group1 = new KeysImpl().createPrivateKey();
		PrivateKey group2 = new KeysImpl().createPrivateKey();
		
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
		expectValues(tuplesA.filter().audience(group1).tuples(), "hey people!");
		expectValues(tuplesA.filter().audience(group2).tuples());
		expectValues(tuplesB.filter().tuples(), "hey B-dog!!");
		expectValues(tuplesB.filter().audience(group1).tuples(), "hey people!");
		expectValues(tuplesB.filter().audience(group2).tuples());
		expectValues(tuplesC.filter().tuples());
		expectValues(tuplesC.filter().audience(group1).tuples(), "hey people!");
		expectValues(tuplesC.filter().audience(group2).tuples());
	}
	
}
