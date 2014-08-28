package sneer.core.tests;

import static sneer.core.tests.ObservableTestUtils.*;

import java.util.*;

import junit.framework.*;
import rx.Observable;
import rx.functions.*;
import rx.observables.*;
import rx.subjects.*;
import sneer.*;
import sneer.admin.*;
import sneer.commons.Arrays;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class ConversationsAPITest extends TestCase {
	
	protected final Object network = newNetwork();

	protected Object newNetwork() {
		return Glue.newNetworkSimulator();
	}

	@Override
	public void tearDown() {
		Glue.tearDownNetwork(network);
	}
	
	
	protected final SneerAdmin adminA = newSneerAdmin();
	protected final SneerAdmin adminB = newSneerAdmin();
	protected final SneerAdmin adminC = newSneerAdmin();

	protected final PublicKey userA = adminA.sneer().self().publicKey().current();
	protected final PublicKey userB = adminB.sneer().self().publicKey().current();
	protected final PublicKey userC = adminC.sneer().self().publicKey().current();
	
	protected final Sneer sneerA = adminA.sneer();
	protected final Sneer sneerB = adminB.sneer();
	protected final Sneer sneerC = adminC.sneer();

	protected PrivateKey newPrivateKey() {
		return Keys.createPrivateKey();
	}
	
	private SneerAdmin newSneerAdmin() {
		return Glue.newSneerAdmin(Keys.createPrivateKey(), network, newTupleBase());
	}

	protected Object newTupleBase() {
		return ReplaySubject.create();
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
		
		sneerA.addContact("Party Boy", partyB);
		assertSame(partyB, sneerA.findContact(partyB).party());
	}

	public void testExceptionOnDuplicatedNickname() throws FriendlyException {

		sneerA.addContact("Party Boy", sneerA.produceParty(userB));
		try {
			sneerA.addContact("Party Boy", sneerA.produceParty(userC));
			fail("should have failed with "+FriendlyException.class.getSimpleName());
		} catch (FriendlyException expected) {
			
		}
	}

	public void testChangeContactNickname() throws FriendlyException {

		Party partyB = sneerA.produceParty(userB);

		sneerA.addContact("Party Boy", partyB);
		
		Observable<String> partyBNicks = sneerA.findContact(partyB).nickname().observable();
		expecting(
			values(partyBNicks, "Party Boy"));

		ReplaySubject<String> nicknames = ReplaySubject.create();		
		partyBNicks.subscribe(nicknames);

		sneerA.findContact(partyB).setNickname("Party Man");

		expecting(
			values(nicknames, "Party Boy", "Party Man"),
			values(partyBNicks, "Party Man"));
	}
	
	public void testContactListSequence() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);
		final Party partyC = sneerA.produceParty(userC);
		
		expecting(
			values(sneerA.contacts(), Collections.emptyList()));
		
		sneerA.addContact("Party Boy", partyB);
		
		expecting(
			contactsOf(sneerA, partyB));
		
		sneerA.addContact("Party Chick", partyC);

		expecting(
			contactsOf(sneerA, partyB, partyC));
	}
	
	public void testContactListRestore() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);
		final Party partyC = sneerA.produceParty(userC);
		sneerA.addContact("Party Boy", partyB);
		sneerA.addContact("Party Chick", partyC);

		expecting(
			contactsOf(restart(adminA).sneer(), partyB, partyC));
	}


	private Observable<Void> contactsOf(final Sneer sneer, final Party... parties) {
		return sneer.contacts().map(new Func1<List<Contact>, Void>() {  @Override public Void call(List<Contact> t1) {
			ObservableTestUtils.assertArrayEquals(
				Arrays.map(parties, new Func1<Party, Contact>() {  @Override public Contact call(Party t1) {
					return sneer.findContact(t1);
				}}),
				t1.toArray());
			return null;
		} });
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
			values(profileB2.preferredNickname(), "Party Man"));
		
	}	
	
	public void testTuplesFromContactsAreVisible() throws FriendlyException {
		
		sneerA.addContact("little b", sneerA.produceParty(userB));
		
		sneerB.tupleSpace().publisher()
			.type("tweet")
			.pub("hello");
		
		expecting(payloads(sneerA.tupleSpace().filter().type("tweet").tuples(), "hello"));
		
	}

	public void testTuplesFromNewContactsAreVisible() throws FriendlyException {
		
		// open twitter client
		ConnectableObservable<Tuple> tweets = sneerA.tupleSpace().filter().type("tweet").tuples().replay();
		tweets.connect();
		
		// future contact publishes a tweet
		sneerB.tupleSpace()
			.publisher()
			.type("tweet")
			.pub("hello");
		
		// it becomes a contact
		sneerA.addContact("little b", sneerA.produceParty(userB));
		
		// tweets should be visible
		expecting(payloads(tweets, "hello"));
		
	}
	
	public void testEmitConversationForEveryContact() throws FriendlyException {

		Party partyBOfA = sneerA.produceParty(userB);
		sneerA.addContact("little b", partyBOfA);

		expecting(
			same(
				sneerA.conversations()
					.flatMapIterable(new Func1<List<Conversation>, Iterable<? extends Conversation>>() {  @Override public Iterable<? extends Conversation> call(List<Conversation> t1) {
						return t1;
					} })
					.map(new Func1<Conversation, Party>() {  @Override public Party call(Conversation t1) {
						return t1.party();
					} }), 
				partyBOfA));
			
		
	}
	
	public void testPartyName() throws FriendlyException {
		
		// 1 - type=sneer/contact party=puk
		// 2 - ? profile/preferred-nickname author=puk
		// 3 - ? profile/preferred-name author=puk
		// 3 - puk
		
		// TODO
		
		Party partyBOfA = sneerA.produceParty(userB);
		sneerA.addContact("little b", partyBOfA);
		
		expecting(
			values(partyBOfA.name(), "little b"));
		
		
	}
	
}
