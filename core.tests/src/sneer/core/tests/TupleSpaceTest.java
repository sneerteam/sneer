package sneer.core.tests;

import static org.junit.Assert.*;

import org.junit.*;

import sneer.tuples.*;

public class TupleSpaceTest extends TupleSpaceTestsBase {
	
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
}
