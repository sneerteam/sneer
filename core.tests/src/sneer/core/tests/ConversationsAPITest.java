package sneer.core.tests;

import junit.framework.TestCase;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.BlockingObservable;
import rx.observables.ConnectableObservable;
import sneer.*;
import sneer.admin.SneerAdmin;
import sneer.commons.Arrays;
import sneer.commons.Clock;
import sneer.commons.exceptions.FriendlyException;
import sneer.crypto.impl.KeysImpl;
import sneer.tuples.Tuple;
import sneer.tuples.TuplePublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static sneer.core.tests.ClojureUtils.var;
import static sneer.commons.Arrays.asList;
import static sneer.core.tests.ObservableTestUtils.*;

public class ConversationsAPITest extends TestCase {

	protected Object network;
	protected Object tupleBaseA;

	protected SneerAdmin adminA;
	protected SneerAdmin adminB;
	protected SneerAdmin adminC;

	protected PublicKey userA;
	protected PublicKey userB;
	protected PublicKey userC;

	protected Sneer sneerA;
	protected Sneer sneerB;
	protected Sneer sneerC;

	@Override
	protected void setUp() {
		network = newNetwork();
		tupleBaseA = newTupleBase();

		adminA = newSneerAdmin(createPrivateKey(), tupleBaseA);
		adminB = newSneerAdmin();
		adminC = newSneerAdmin();

		userA = adminA.sneer().self().publicKey().current();
		userB = adminB.sneer().self().publicKey().current();
		userC = adminC.sneer().self().publicKey().current();

		sneerA = adminA.sneer();
		sneerB = adminB.sneer();
		sneerC = adminC.sneer();

		Clock.startMocking();
	}

	@Override
	public void tearDown() {
		Glue.tearDownNetwork(network);
		Clock.stopMocking();
	}



	private SneerAdmin newSneerAdmin() {
		return newSneerAdmin(createPrivateKey(), newTupleBase());
	}

	private PrivateKey createPrivateKey() {
		return new KeysImpl().createPrivateKey();
	}

	private SneerAdmin newSneerAdmin(PrivateKey prik, Object tupleBase) {
		Glue.networkConnect(network, prik.publicKey(), tupleBase);
		return (SneerAdmin) var("sneer.admin", "new-sneer-admin").invoke(prik, tupleBase);
	}

	private static Object newNetwork() {
		return var("sneer.core.tests.local-server-network", "start-local").invoke();
	}

	private static Object newTupleBase() {
		return Glue.newPersistentTupleBase();
	}

	public void testSameSneer() {
		assertEquals(adminA.sneer(), adminA.sneer());
	}


	public void testPukOfParty() {

		Party someone = sneerA.produceParty(userB);

		assertEquals(userB, someone.publicKey().current());

	}

	public void testAlwaysReturnsSamePartyInstance() {

		Party someoneElse = sneerA.produceParty(userB);

		assertSame(someoneElse, sneerA.produceParty(userB));

	}

	public void testProduceContact() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);

		Contact contact = sneerA.findContact(partyB);
		assertNull(contact);

		sneerA.produceContact("Party Boy", partyB, null);
		assertSame(partyB, sneerA.findContact(partyB).party().current());
	}


	public void testExceptionOnDuplicatedNickname() throws FriendlyException {

		sneerA.produceContact("Party Boy", sneerA.produceParty(userB), null);
		try {
			sneerA.produceContact("Party Boy", sneerA.produceParty(userC), null);
			fail("should have failed with " + FriendlyException.class.getSimpleName());
		} catch (FriendlyException expected) {}
	}


	public void testChangeContactNickname() throws FriendlyException {
		Party party = sneerA.produceParty(userB);

		sneerA.produceContact("Party Boy", party, null);

		Observable<String> nicks = sneerA.findContact(party).nickname().observable();
		expecting(
			values(nicks, "Party Boy"));

		sneerA.findContact(party).setNickname("Party Man");
		expecting(
			values(nicks, "Party Man"));
	}


	public void testChangeContactNicknamePersistence() throws FriendlyException {
		Party party = sneerA.produceParty(userB);
		sneerA.produceContact("Party Boy", party, null);

		Contact contact = sneerA.findContact(party);
		contact.setNickname("Party Man");

		Sneer newSneer = newSneerAdmin(adminA.privateKey(), tupleBaseA).sneer();
		Party newParty = newSneer.produceParty(userB);
		assertEquals("Party Man", newSneer.findContact(newParty).nickname().current());
	}


	public void testProblemWithNewNickname() throws FriendlyException {
		//assertNotNull(sneerA.problemWithNewNickname(""));

		Party partyB = sneerA.produceParty(userB);
		Party partyC = sneerA.produceParty(userC);

		assertNull   (sneerA.problemWithNewNickname("Party Boy", partyB));
		sneerA.produceContact("Party Boy", partyB, null);
		assertNull   (sneerA.problemWithNewNickname("Party Boy", partyB));
		assertNotNull(sneerA.problemWithNewNickname("Party Boy", partyC));

		try {
			sneerA.produceContact("Party Boy", partyC, null);
			fail();
		} catch (FriendlyException expected) {}

		try {
			sneerA.produceContact("Party Boy2", partyB, null);
			fail();
		} catch (FriendlyException expected) {}

		sneerA.produceContact("Party Chick", partyC, null);
		Contact chick = sneerA.findContact(partyC);
		try {
			chick.setNickname("Party Boy");
			fail();
		} catch (FriendlyException expected) {}

		chick.setNickname("Party Chick 2");
	}


	public void testContactListSequence() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);
		final Party partyC = sneerA.produceParty(userC);

		expecting(
			values(sneerA.contacts(), Collections.emptyList()));

		sneerA.produceContact("Party Boy", partyB, null);

		expecting(
			contactsOf(sneerA, partyB));

		sneerA.produceContact("Party Chick", partyC, null);

		expecting(
			contactsOf(sneerA, partyB, partyC));
	}

	public void testContactListRestore() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);
		final Party partyC = sneerA.produceParty(userC);
		sneerA.produceContact("Party Boy", partyB, null);
		sneerA.produceContact("Party Chick", partyC, null);

		expecting(
			contactsOf(restart(adminA).sneer(), partyB, partyC));
	}


	private Observable<Void> contactsOf(final Sneer sneer, final Party... parties) {
		return sneer.contacts().map(new Func1<List<Contact>, Void>() {  @Override public Void call(List<Contact> contacts) {
			ObservableTestUtils.assertArrayEquals(
				Arrays.map(parties, new Func1<Party, Contact>() {  @Override public Contact call(Party party) {
					return sneer.findContact(party);
				}}),
				contacts.toArray());
			return null;
		}});
	}


	public void ignoreTestTuplesFromContactsAreVisible() throws FriendlyException {

		sneerA.produceContact("little b", sneerA.produceParty(userB), null);

		sneerB.tupleSpace().publisher()
			.type("tweet")
			.pub("hello");

		expecting(payloads(sneerA.tupleSpace().filter().type("tweet").tuples(), "hello"));
	}


	public void ignoreTestTuplesFromNewContactsAreVisible() throws FriendlyException {
		// open twitter client
		ConnectableObservable<Tuple> tweets = sneerA.tupleSpace().filter().type("tweet").tuples().replay();
		tweets.connect();

		// future contact publishes a tweet
		sneerB.tupleSpace()
			.publisher()
			.type("tweet")
			.pub("hello");

		// it becomes a contact
		sneerA.produceContact("little b", sneerA.produceParty(userB), null);

		// tweets should be visible
		expecting(payloads(tweets, "hello"));

	}


	public void testEmitConversationForEveryContact() throws FriendlyException {

		Party partyBOfA = sneerA.produceParty(userB);
		sneerA.produceContact("little b", partyBOfA, null);

		expecting(
			same(
				flatMapConversationsOf(sneerA).map(new Func1<Conversation, Party>() {  @Override public Party call(Conversation conversation) {
					return conversation.contact().party().current();
				}}),
				partyBOfA));

	}


	private Observable<Conversation> flatMapConversationsOf(Sneer sneer) {
		return sneer.conversations().all()
			.flatMapIterable(
					new Func1<List<Conversation>, Iterable<? extends Conversation>>() {
						@Override
						public Iterable<? extends Conversation> call(List<Conversation> conversations) {
							return conversations;
						}
					});
	}


	public void testConversationMessageSequence() throws Exception {

		Party pAB = sneerA.produceParty(userB);
		sneerA.produceContact("b", pAB, null);
		Conversation cAB = conversationWith(pAB, sneerA);

		Party pBA = sneerB.produceParty(userA);
		sneerB.produceContact("a", pBA, null);
		Conversation cBA = conversationWith(pBA, sneerB);

		cAB.sendMessage("Hello1");
		messagesEventually(cBA, "Hello1");
		Clock.tick();
		cBA.sendMessage("Hello2");
		messagesEventually(cAB, "Hello1", "Hello2");
		Clock.tick();
		cAB.sendMessage("Hello3");
		messagesEventually(cBA, "Hello1", "Hello2", "Hello3");

		//Restart
		Sneer newSneer = newSneerAdmin(adminA.privateKey(), tupleBaseA).sneer();
		Party newB = newSneer.produceParty(userB);
		Conversation newConversation = conversationWith(newB, newSneer);
		messagesEventually(newConversation, "Hello1", "Hello2", "Hello3");
	}

	private Conversation conversationWith(Party party, Sneer sneer) {
		return sneer.conversations().withParty(party);
	}


	public void testUnreadMessageCount() throws Exception {

		Party pAB = sneerA.produceParty(userB);
		sneerA.produceContact("b", pAB, null);
		Conversation cAB = conversationWith(pAB, sneerA);

		Party pBA = sneerB.produceParty(userA);
		sneerB.produceContact("a", pBA, null);
		final Conversation cBA = conversationWith(pBA, sneerB);

		expecting(eventually(cBA.unreadMessageCount(), 0L));

		cAB.sendMessage("Hello1 - read");
		Iterator<List<ConversationItem>> items = cBA.items().toBlocking().getIterator();
		System.out.println("- - - - Message: " + items.next());
		System.out.println("- - - - Message: " + items.next());
		System.out.println("- - - - Message: " + items.next());

		expecting(eventually(cBA.unreadMessageCount(), 1L));

		cBA.items().subscribe(new Action1<List<ConversationItem>>() { @Override public void call(List<ConversationItem> messages) {
			if (messages.isEmpty())
				return;
			Message last = (Message) messages.get(messages.size() - 1);
			System.out.println("- - - - LAST: " + last);
			if (last.label().contains("read"))
				cBA.setRead(last);
		}});

		expecting(eventually(cBA.unreadMessageCount(), 0L));

		cAB.sendMessage("Hello2");
		expecting(eventually(cBA.unreadMessageCount(), 1L));

		cAB.sendMessage("Hello3");
		cAB.sendMessage("Hello4");
		expecting(eventually(cBA.unreadMessageCount(), 3L));

		cAB.sendMessage("Hello5 - read");
		expecting(eventually(cBA.unreadMessageCount(), 0L));

	}


	private void messagesEventually(Conversation conversation, String... msgsExpected) {
		expecting(eventually(conversation.items().map(toMessageContentList()), asList(msgsExpected)));
	}


	public void ignoreTestMessageLabel() {
		TuplePublisher publisher = sneerA.tupleSpace().publisher()
			.audience(userB)
			.type("message");

		publisher.field("label", "mylabel").pub();
		Clock.tick();
		publisher.field("label", "mylabel2").pub();
		Clock.tick();
		publisher.field("label", "mylabel3").pub();

		Observable<String> contents = conversationWith(sneerA.produceParty(userB), sneerA)
			.items()
			.flatMapIterable(new Func1<List<ConversationItem>, Iterable<? extends ConversationItem>>() { @Override public Iterable<? extends ConversationItem> call(List<ConversationItem> messages) {
				return messages;
			}})
			.map(new Func1<ConversationItem, String>() { @Override public String call(ConversationItem message) {
				return message.label().toString();
			}});

		expecting(values(contents, "mytext", "mytext2", "mytext3"));
	}


	private Func1<? super List<ConversationItem>, ? extends List<Object>> toMessageContentList() {
		return new Func1<List<ConversationItem>, List<Object>>() {  @Override public List<Object> call(List<ConversationItem> messages) {
			ArrayList<Object> r = new ArrayList<Object>(messages.size());
			for (ConversationItem m : messages) r.add(m.label());
			return r;
		}};
	}


	protected Func1<List<?>,Boolean> isEmpty() {
		return new Func1<List<?>, Boolean>() { @Override public Boolean call(List<?> stuff) {
			return stuff.isEmpty();
		}};
	}


	private SneerAdmin restart(SneerAdmin admin) {
		return (SneerAdmin) var("sneer.restartable", "restart").invoke(admin);
	}

}
