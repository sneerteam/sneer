package sneer.core.tests;

import static org.junit.Assert.*;
import static sneer.core.tests.ObservableTestUtils.*;

import java.util.*;

import org.junit.*;

import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;
import sneer.admin.*;
import sneer.commons.exceptions.*;

public class ConversationsAPITest extends ConversationsAPITestsBase {

	@Test
	public void sameSneer() {
		assertEquals(adminA.sneer(), adminA.sneer());
	}


	@Test
	public void pukOfParty() {

		Party someone = sneerA.produceParty(userB);

		assertEquals(userB, someone.publicKey().current());

	}

	@Test
	public void alwaysReturnsSamePartyInstance() {

		Party someoneElse = sneerA.produceParty(userB);

		assertSame(someoneElse, sneerA.produceParty(userB));

	}

	@Test
	public void addContact() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);

		Contact contact = sneerA.findContact(partyB);
		assertNull(contact);
		
		sneerA.addContact("Party Boy", partyB);
		assertSame(partyB, sneerA.findContact(partyB).party());
	}

	@Test(expected = FriendlyException.class)
	public void exceptionOnDuplicatedNickname() throws FriendlyException {

		sneerA.addContact("Party Boy", sneerA.produceParty(userB));
		sneerA.addContact("Party Boy", sneerA.produceParty(userC));
	}

	@Test
	public void changeContactNickname() throws FriendlyException {

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
	
	@Test
	public void contactListSequence() throws FriendlyException {

		final Party partyB = sneerA.produceParty(userB);
		final Party partyC = sneerA.produceParty(userC);
		
		expecting(
			values(sneerA.contacts(), Collections.emptyList()));
		
		sneerA.addContact("Party Boy", partyB);
		
		expecting(
			sneerA.contacts().map(new Func1<List<Contact>, Void>() {  @Override public Void call(List<Contact> t1) {
				assertArrayEquals(new Contact[]{sneerA.findContact(partyB)}, t1.toArray());
				return null;
			} }));
		
		
		sneerA.addContact("Party Chick", partyC);

		expecting(
			sneerA.contacts().map(new Func1<List<Contact>, Void>() {  @Override public Void call(List<Contact> t1) {
				assertArrayEquals(new Contact[]{sneerA.findContact(partyB), sneerA.findContact(partyC)}, t1.toArray());
				return null;
			} }));
	}
	
	
	@Test
	public void preferredNickname() {
		
		Profile profileBFromB = sneerB.profileFor(sneerB.self());
		Profile profileBFromA = sneerA.profileFor(sneerA.produceParty(userB));
		
		profileBFromB.setPreferredNickname("Party Boy");
		
		expecting(
			values(profileBFromA.preferredNickname(), "Party Boy"));
		
		profileBFromB.setPreferredNickname("Party Man");

		expecting(
			values(profileBFromA.preferredNickname(), "Party Man"));
		
	}
	
	@Test
	public void preferredNicknameForSelf() {
		
		Profile profileB = sneerB.profileFor(sneerB.self());
		profileB.setPreferredNickname("Party Boy");		
		expecting(
			values(profileB.preferredNickname(), "Party Boy"));
		
		SneerAdmin adminB2 = Glue.restart(adminB);
		Sneer sneerB2 = adminB2.sneer();
		Profile profileB2 = sneerB2.profileFor(sneerB2.self());
		expecting(
			values(profileB2.preferredNickname(), "Party Boy"));
		
		profileB2.setPreferredNickname("Party Man");
		expecting(
			values(profileB2.preferredNickname(), "Party Man"));
		
	}

}
