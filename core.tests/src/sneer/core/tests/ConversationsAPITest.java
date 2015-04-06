package sneer.core.tests;

import junit.framework.TestCase;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
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


	public void testSameProfile() {
		assertEquals(sneerA.profileFor(sneerA.self()), sneerA.profileFor(sneerA.self()));
	}


	public void testPukOfParty() {

		Party someone = sneerA.produceParty(userB);

		assertEquals(userB, someone.publicKey().current());

	}

	public void testAlwaysReturnsSamePartyInstance() {

		Party someoneElse = sneerA.produceParty(userB);

		assertSame(someoneElse, sneerA.produceParty(userB));

	}

	public void testAddContact() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);

		Contact contact = sneerA.findContact(partyB);
		assertNull(contact);

		sneerA.addContact("Party Boy", partyB, null);
		assertSame(partyB, sneerA.findContact(partyB).party());
	}


	public void testExceptionOnDuplicatedNickname() throws FriendlyException {

		sneerA.addContact("Party Boy", sneerA.produceParty(userB), null);
		try {
			sneerA.addContact("Party Boy", sneerA.produceParty(userC), null);
			fail("should have failed with " + FriendlyException.class.getSimpleName());
		} catch (FriendlyException expected) {}
	}


	public void testChangeContactNickname() throws FriendlyException {
		Party party = sneerA.produceParty(userB);

		sneerA.addContact("Party Boy", party, null);

		Observable<String> nicks = sneerA.findContact(party).nickname().observable();
		expecting(
			values(nicks, "Party Boy"));

		sneerA.findContact(party).setNickname("Party Man");
		expecting(
			values(nicks, "Party Man"));
	}


	public void testChangeContactNicknamePersistence() throws FriendlyException {
		Party party = sneerA.produceParty(userB);
		sneerA.addContact("Party Boy", party, null);

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
		sneerA.addContact("Party Boy", partyB, null);
		assertNull   (sneerA.problemWithNewNickname("Party Boy", partyB));
		assertNotNull(sneerA.problemWithNewNickname("Party Boy", partyC));

		try {
			sneerA.addContact("Party Boy", partyC, null);
			fail();
		} catch (FriendlyException expected) {}

		try {
			sneerA.addContact("Party Boy2", partyB, null);
			fail();
		} catch (FriendlyException expected) {}

		sneerA.addContact("Party Chick", partyC, null);
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

		sneerA.addContact("Party Boy", partyB, null);

		expecting(
			contactsOf(sneerA, partyB));

		sneerA.addContact("Party Chick", partyC, null);

		expecting(
			contactsOf(sneerA, partyB, partyC));
	}

	public void testContactListRestore() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);
		final Party partyC = sneerA.produceParty(userC);
		sneerA.addContact("Party Boy", partyB, null);
		sneerA.addContact("Party Chick", partyC, null);

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

	public void testPreferredNickname() {

		Profile profileBFromB = sneerB.profileFor(sneerB.self());
		Profile profileBFromA = sneerA.profileFor(sneerA.produceParty(userB));

		profileBFromB.setPreferredNickname("Party Boy");

		expecting(
			values(profileBFromA.preferredNickname(), "Party Boy"));

		profileBFromB.setPreferredNickname("Party Man");

		expecting(
			eventually(profileBFromA.preferredNickname(), "Party Man"));

	}

	public void testPreferredNicknameForSelf() {

		Profile profileB = sneerB.profileFor(sneerB.self());
		profileB.setPreferredNickname("Party Boy");
		expecting(
			values(profileB.preferredNickname(), "Party Boy"));

		SneerAdmin adminB2 = restart(adminB);
		Sneer sneerB2 = adminB2.sneer();
		Profile profileB2 = sneerB2.profileFor(sneerB2.self());
		expecting(
			values(profileB2.preferredNickname(), "Party Boy"));

		profileB2.setPreferredNickname("Party Man");
		expecting(
			eventually(profileB2.preferredNickname(), "Party Man"));

	}


	public void testIsOwnNameLocallyAvailable() {

		Profile profile = sneerA.profileFor(sneerA.self());

		assertEquals(false, profile.isOwnNameLocallyAvailable());

		profile.setOwnName("neide");

		assertEquals(true, profile.isOwnNameLocallyAvailable());
	}


	public void ignoreTestTuplesFromContactsAreVisible() throws FriendlyException {

		sneerA.addContact("little b", sneerA.produceParty(userB), null);

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
		sneerA.addContact("little b", sneerA.produceParty(userB), null);

		// tweets should be visible
		expecting(payloads(tweets, "hello"));

	}


	public void testEmitConversationForEveryContact() throws FriendlyException {

		Party partyBOfA = sneerA.produceParty(userB);
		sneerA.addContact("little b", partyBOfA, null);

		expecting(
			same(
				flatMapConversationsOf(sneerA).map(new Func1<Conversation, Party>() {  @Override public Party call(Conversation conversation) {
					return conversation.party();
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
		sneerA.addContact("b", pAB, null);
		Conversation cAB = conversationWith(pAB, sneerA);

		Party pBA = sneerB.produceParty(userA);
		sneerB.addContact("a", pBA, null);
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
		return sneer.conversations().with(party);
	}


	public void testUnreadMessageCount() throws Exception {

		Party pAB = sneerA.produceParty(userB);
		sneerA.addContact("b", pAB, null);
		Conversation cAB = conversationWith(pAB, sneerA);

		Party pBA = sneerB.produceParty(userA);
		sneerB.addContact("a", pBA, null);
		final Conversation cBA = conversationWith(pBA, sneerB);

		expecting(eventually(cBA.unreadMessageCount(), 0L));

		cAB.sendMessage("Hello1 - read");
		expecting(eventually(cBA.unreadMessageCount(), 1L));

		cBA.messages().subscribe(new Action1<List<Message>>() { @Override public void call(List<Message> messages) {
			if (messages.isEmpty())
				return;
			Message last = messages.get(messages.size() - 1);
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
		expecting(eventually(conversation.messages().map(toMessageContentList()), asList(msgsExpected)));
	}


	public void testPartyName() throws FriendlyException {

		// 1 - type=contact party=puk
		// 2 - ? profile/preferred-nickname author=puk
		// 3 - ? profile/preferred-name author=puk
		// 3 - puk

		// TODO

		Party partyBOfA = sneerA.produceParty(userB);
		sneerA.addContact("little b", partyBOfA, null);

		expecting(
			values(partyBOfA.name(), "little b"));

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
			.messages()
			.flatMapIterable(new Func1<List<Message>, Iterable<? extends Message>>() { @Override public Iterable<? extends Message> call(List<Message> messages) {
				return messages;
			}})
			.map(new Func1<Message, String>() { @Override public String call(Message message) {
				return message.label().toString();
			}});

		expecting(values(contents, "mytext", "mytext2", "mytext3"));
	}


	private Func1<? super List<Message>, ? extends List<Object>> toMessageContentList() {
		return new Func1<List<Message>, List<Object>>() {  @Override public List<Object> call(List<Message> messages) {
			ArrayList<Object> r = new ArrayList<Object>(messages.size());
			for (Message m : messages) r.add(m.label());
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
