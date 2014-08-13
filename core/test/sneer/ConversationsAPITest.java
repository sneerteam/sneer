package sneer;

import static core.ObservableTestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import rx.subjects.*;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;

public class ConversationsAPITest extends ConversationsAPITestsBase {

	@Test
	public void changesPrik() {

		assertEquals(prikA, adminA.privateKey());

		PrivateKey anotherPrikInTheFireWall = Keys.createPrivateKey();
		adminA.initialize(anotherPrikInTheFireWall);

		assertEquals(anotherPrikInTheFireWall, adminA.privateKey());
	}

	@Test
	public void myOwnPublicKey() {

		assertEquals(prikA.publicKey(), sneerA.self().publicKey().current());

		assertEqualsUntilNow(sneerA.self().publicKey().observable(), prikA.publicKey());

	}

	@Test
	public void pukOfParty() {

		Party someone = sneerA.produceParty(prikB.publicKey());

		assertEquals(prikB.publicKey(), someone.publicKey().current());

	}

	@Test
	public void alwaysReturnsSamePartyInstance() {

		Party someoneElse = sneerA.produceParty(prikB.publicKey());

		assertSame(someoneElse, sneerA.produceParty(prikB.publicKey()));

	}

	@Test
	public void addContact() throws FriendlyException {

		Party partyB = sneerA.produceParty(prikB.publicKey());

		assertNull(sneerA.findContact(partyB));

		sneerA.addContact("Party Boy", partyB);

		Contact contactB = sneerA.findContact(partyB);
		assertNotNull(contactB);
		assertSame(partyB, contactB.party());
	}

	@Test(expected = FriendlyException.class)
	public void exceptionOnDuplicatedNickname() throws FriendlyException {

		sneerA.addContact("Party Boy", sneerA.produceParty(prikB.publicKey()));
		sneerA.addContact("Party Boy", sneerA.produceParty(prikC.publicKey()));
	}

	@Test
	public void changeContactNickname() throws FriendlyException {

		Party partyB = sneerA.produceParty(prikB.publicKey());

		sneerA.addContact("Party Boy", partyB);

		Contact contactB = sneerA.findContact(partyB);

		assertEquals("Party Boy", contactB.nickname().current());

		ReplaySubject<String> nicknames = ReplaySubject.create();

		contactB.nickname().observable().subscribe(nicknames);

		sneerA.addContact("Party Man", partyB);

		assertEqualsUntilNow(nicknames, "Party Boy", "Party Man");

		assertEqualsUntilNow(contactB.nickname().observable(), "Party Man");
	}
	
	@Test
	public void contactListSequence() throws FriendlyException {

		Party partyB = sneerA.produceParty(prikB.publicKey());
		Party partyC = sneerA.produceParty(prikC.publicKey());
		
		assertEqualsUntilNow(sneerA.contacts());
		
		sneerA.addContact("Party Boy", partyB);

		assertEqualsUntilNow(sneerA.contacts(), Arrays.asList(sneerA.findContact(partyB)));
		
		sneerA.addContact("Party Boy", partyC);

		assertEqualsUntilNow(sneerA.contacts(), Arrays.asList(sneerA.findContact(partyB), sneerA.findContact(partyC)));
		
	}
	
	
	@Test
	public void preferredNickname() {
		
		Profile profileBFromB = sneerB.profileFor(sneerB.self());
		Profile profileBFromA = sneerA.profileFor(sneerA.produceParty(prikB.publicKey()));
		
		profileBFromB.setPreferredNickname("Party Boy");
		
		assertEqualsUntilNow(profileBFromA.preferredNickname(), "Party Boy");
		
		profileBFromB.setPreferredNickname("Party Man");

		assertEqualsUntilNow(profileBFromA.preferredNickname(), "Party Man");
		
	}

}
