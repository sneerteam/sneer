package sneer.core.tests;

import static junit.framework.Assert.assertEquals;
import static sneer.core.tests.ObservableTestUtils.assertList;
import static sneer.core.tests.ObservableTestUtils.expecting;
import static sneer.core.tests.ObservableTestUtils.field;
import static sneer.core.tests.ObservableTestUtils.notifications;
import static sneer.core.tests.ObservableTestUtils.payloads;
import static sneer.core.tests.ObservableTestUtils.values;
import static sneer.tuples.Tuple.TO_TYPE;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import sneer.PrivateKey;
import sneer.crypto.impl.KeysImpl;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.tuples.TuplePublisher;
import sneer.tuples.TupleSpace;

public class PubSubTest extends TupleSpaceTestsBase {

	public PubSubTest() {}

	protected PubSubTest(Func0<Object> tupleBaseFactory) {
		super(tupleBaseFactory);
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

		TupleFilter subscriber = tuplesB.filter().author(userA.publicKey());
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
			values(tuplesB.filter().author(userA.publicKey()).tuples().map(TO_TYPE), "rock-paper-scissor/move", "rock-paper-scissor/message"));

	}

	@Test
	public void targetUser() {

		tuplesA.publisher()
			.audience(userC.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");

		tuplesA.publisher()
			.type("rock-paper-scissor/move")
			.pub("end");

		expecting(
			payloads(tuplesA.filter().author(userA.publicKey()).type("rock-paper-scissor/move").tuples(), "paper", "end"),
			payloads(tuplesB.filter().author(userA.publicKey()).type("rock-paper-scissor/move").tuples(), "end"),
			payloads(tuplesC.filter().author(userA.publicKey()).type("rock-paper-scissor/move").tuples(), "paper", "end"));
	}

	@Test
	@Ignore
	public void publicTuples() {

		String name = "UserA McCloud";
		tuplesA.publisher()
			.type("profile/name")
			.pub(name);

		expecting(
			payloads(profileName(tuplesC), name),
			payloads(profileName(tuplesB), name),
			payloads(profileName(tuplesA), name));
	}

	private Observable<Tuple> profileName(TupleSpace tupleSpace) {
		return tupleSpace.filter().type("profile/name").tuples();
	}

	@Test
	public void newPeerCanSubscribeToPastTuples() {

		String name = "UserA McCloud";
		tuplesA.publisher()
			.type("profile/name")
			.pub(name);

		PrivateKey userD = new KeysImpl().createPrivateKey();
		TupleSpace tuplesD = newTupleSpace(userD, followees(userA));

		expecting(
			payloads(tuplesD.filter().author(userA.publicKey()).tuples(), name));
	}

	@Test
	public void byAuthor() {
		tuplesA.publisher()
			.type("user/name")
			.pub("UserA McCloud");
		tuplesB.publisher()
			.type("user/name")
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

		tuplesA.publisher()
			.audience(userB.publicKey())
			.type("sentinel")
			.pub("eof");

		expecting(
			payloads(tuplesB.filter().author(userA.publicKey()).audience(userB).tuples(), "eof"));
	}

	@Test
	public void customFieldTypeRepresentation() {
		TuplePublisher customPublisher = tuplesA.publisher().type("banana-type");
		customPublisher.field("custom", 42).pub();
		customPublisher.field("custom", 23).pub();

		expecting(
			values(
				tuplesA
					.filter()
					.field("custom", 42)
					.tuples()
					.map(field("custom")),
				42L));
	}

	@Test
	public void payloadTypeRepresentation() {
		tuplesA.publisher().type("banana-type")
			.pub(42);

		expecting(
			payloads(
				tuplesA
					.filter()
					.tuples(),
				42L));
	}

	@Test
	public void completedLocalTuples() {

		TuplePublisher publisher = tuplesA.publisher()
			.audience(userA.publicKey())
			.type("profile/name");

		publisher.pub("old name");
		publisher.pub("new name");

		expecting(
			notifications(
				tuplesA.filter()
					.audience(userA)
					.type("profile/name")
					.localTuples()
					.map(Tuple.TO_PAYLOAD),
				Notification.createOnNext("old name"),
				Notification.createOnNext("new name"),
				Notification.createOnCompleted()));
	}

	@Test
	public void localTupleQueriesAreDeferredUntilSubscribe() {

		TuplePublisher publisher = tuplesA.publisher().type("local");
		publisher.pub("before");

		Observable<Object> localPayloads = tuplesA.filter()
				.type("local")
				.localTuples()
				.map(Tuple.TO_PAYLOAD);

		publisher.pub("after");

		expecting(
			notifications(
				localPayloads,
				Notification.createOnNext("before"),
				Notification.createOnNext("after"),
				Notification.createOnCompleted()));

		publisher.pub("later");

		expecting(
			notifications(
				localPayloads,
				Notification.createOnNext("before"),
				Notification.createOnNext("after"),
				Notification.createOnNext("later"),
				Notification.createOnCompleted()));
	}

	@Test
	public void subscriberCriteriaWithArray() {
		final Object[] array = {"notes", userB.publicKey()};

		tuplesA.publisher()
			.audience(userA.publicKey())
			.type("file")
			.field("path", array)
			.pub("userB is cool");

		Observable<Tuple> actual = tuplesA.filter()
				.audience(userA)
				.type("file")
				//.field("path", array)
				.tuples();

		expecting(
			actual.map(new Func1<Tuple, Void>() { @SuppressWarnings({ "unchecked", "deprecation" }) @Override public Void call(Tuple tuple) {
				assertList(array, ((List<Object>)tuple.get("path")));
				assertEquals("userB is cool", tuple.payload());
				return null;
			}}));
	}

	@Test
	public void receiveTuplesFromAllContacts() {
		tuplesB.publisher()
			.audience(userA.publicKey())
			.type("bla")
			.pub("from b");

		tuplesC.publisher()
			.audience(userA.publicKey())
			.type("bla")
			.pub("from c");

		final TupleFilter byAudience = tuplesA.filter().audience(userA);
		expecting(
			payloads(
				byAudience.author(userB.publicKey()).tuples(),
				"from b"),
			payloads(
				byAudience.author(userC.publicKey()).tuples(),
				"from c"));
	}

	@Test
	public void timeReceived() {
		Tuple tuple = tuplesA.publisher()
			.audience(userA.publicKey())
			.type("bla")
			.pub("bla")
			.toBlocking()
			.first();

		Observable<Long> times = tuplesA.filter().tuples()
				.map(new Func1<Tuple, Long>() {  @Override public Long call(Tuple tuple) {
					return tuple.timestampCreated();
				}});

		expecting(
				values(times, tuple.timestampCreated()));
	}

//	@Test
//	public void pubIfEmpty() {
//
//		TuplePublisher publisher = tuplesA.publisher()
//			.audience(userA.publicKey())
//			.type("profile.nickname");
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
//			.type("party")
//			.localTuples()
//			.flatMap(new Func1<Tuple, Observable<Tuple>>() {  @Override public Observable<Tuple> call(Tuple contact) {
//				return tuplesA.filter()
//						.author(userA.publicKey())
//						.type("contact")
//						.field("partyId", contact.get("partyId"))
//						.localTuples()
//						.last();
//			}})
//			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple tuple) {
//				return !tuple.containsKey("deleted");
//			}})
//			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple tuple) {
//
//				System.out.println("party id: " + tuple.get("partyId") +  ", nickname: " + tuple.payload());
//			}});
//
//
//		Observable<String> allNicknames = tuplesA.filter()
//			.author(userA.publicKey())
//			.type("contact")
//			.localTuples()
//			.groupBy(new Func1<Tuple, PartyId>() {  @Override public PartyId call(Tuple tuple) {
//				return (PartyId) tuple.get("partyId");
//			}})
//			.flatMap(new Func1<GroupedObservable<PartyId, Tuple>, Observable<Tuple>>() {  @Override public Observable<Tuple> call(GroupedObservable<PartyId, Tuple> group) {
//				return group.last();
//			}})
//			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple tuple) {
//				return !tuple.containsKey("deleted");
//			}})
//			.map(new Func1<Tuple, Object>() {  @Override public Object call(Tuple tuple) {
//				return tuple.get("nickname");
//			}})
//			.cast(String.class);
//
//		allNicknames
//			.filter(new Func1<String, Boolean>() {  @Override public Boolean call(String str) {
//				return str.equals("new nickname");
//			}});
//
//
////			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple tuple) {
////				System.out.println("party id: " + tuple.get("partyId") +  ", last-known-puk: " + tuple.get("puk") + ", nickname: " + tuple.get("nickname"));
////			}});
//
//
//		tuplesA.filter()
//			.author(userA.publicKey())
//			.type("party")
//			.localTuples()
//			.groupBy(new Func1<Tuple, PartyId>() {  @Override public PartyId call(Tuple tuple) {
//				return (PartyId) tuple.get("partyId");
//			}})
//			.flatMap(new Func1<GroupedObservable<PartyId, Tuple>, Observable<Tuple>>() {  @Override public Observable<Tuple> call(GroupedObservable<PartyId, Tuple> group) {
//				return group.last();
//			}})
//			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple tuple) {
//				System.out.println("party id: " + tuple.get("partyId") +  ", last-known-puk: " + tuple.get("puk") + ", nickname: " + tuple.get("nickname"));
//			}});
//
//	}

}
