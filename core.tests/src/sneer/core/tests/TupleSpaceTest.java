package sneer.core.tests;

import org.junit.Test;

import rx.Notification;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.tuples.TuplePublisher;

import static org.junit.Assert.assertNotSame;
import static sneer.core.tests.ObservableTestUtils.*;

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

	@Test
	public void lastLocalTuple() {
		TuplePublisher publisher = tuplesA.publisher().audience(userA.publicKey()).type("a-tuple-type");
		publisher.pub("first");
		publisher.pub("last");
		expecting(
			notifications(
				tuplesA
					.filter()
					.type("a-tuple-type")
					.last()
					.localTuples()
					.map(Tuple.TO_PAYLOAD),
				Notification.createOnNext("last"),
				Notification.createOnCompleted()));
	}
}
