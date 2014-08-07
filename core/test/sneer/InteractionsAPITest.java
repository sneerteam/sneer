package sneer;

import static core.ObservableTestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import rx.subjects.*;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;

public class InteractionsAPITest extends InteractionsAPITestsBase {

	@Test
	public void changesPrik() {

		assertEquals(prikA, adminA.privateKey());

		PrivateKey anotherPrikInTheFireWall = Keys.createPrivateKey();
		adminA.initialize(anotherPrikInTheFireWall);

		assertEquals(anotherPrikInTheFireWall, adminA.privateKey());
	}

	@Test
	public void myOwnPublicKey() {

		assertEquals(prikA.publicKey(), sneerA.self().publicKey().mostRecent());

		assertEqualsUntilNow(sneerA.self().publicKey().observable(), prikA.publicKey());

	}

	@Test
	public void pukOfParty() {

		Party someone = sneerA.produceParty(prikB.publicKey());

		assertEquals(prikB.publicKey(), someone.publicKey().mostRecent());

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

		sneerA.setContact("Party Boy", partyB);

		Contact contactB = sneerA.findContact(partyB);
		assertNotNull(contactB);
		assertSame(partyB, contactB.party());
	}

	@Test(expected = FriendlyException.class)
	public void exceptionOnDuplicatedNickname() throws FriendlyException {

		sneerA.setContact("Party Boy", sneerA.produceParty(prikB.publicKey()));
		sneerA.setContact("Party Boy", sneerA.produceParty(prikC.publicKey()));
	}

	@Test
	public void changeContactNickname() throws FriendlyException {

		Party partyB = sneerA.produceParty(prikB.publicKey());

		sneerA.setContact("Party Boy", partyB);

		Contact contactB = sneerA.findContact(partyB);

		assertEquals("Party Boy", contactB.nickname().mostRecent());

		ReplaySubject<String> nicknames = ReplaySubject.create();

		contactB.nickname().observable().subscribe(nicknames);

		sneerA.setContact("Party Man", partyB);

		assertEqualsUntilNow(nicknames, "Party Boy", "Party Man");

		assertEqualsUntilNow(contactB.nickname().observable(), "Party Man");
	}
	
	@Test
	public void contactListSequence() throws FriendlyException {

		Party partyB = sneerA.produceParty(prikB.publicKey());
		Party partyC = sneerA.produceParty(prikC.publicKey());
		
		assertEqualsUntilNow(sneerA.contacts());
		
		sneerA.setContact("Party Boy", partyB);

		assertEqualsUntilNow(sneerA.contacts(), Arrays.asList(sneerA.findContact(partyB)));
		
		sneerA.setContact("Party Boy", partyC);

		assertEqualsUntilNow(sneerA.contacts(), Arrays.asList(sneerA.findContact(partyB), sneerA.findContact(partyC)));
		
	}

}
