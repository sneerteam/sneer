package sneer.core.tests;

import static org.junit.Assert.*;
import static sneer.core.tests.ObservableTestUtils.*;

import java.util.*;

import org.junit.*;

import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;
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

		Observable<Contact> contactQuery = sneerA.findContact(partyB);
		
		sneerA.addContact("Party Boy", partyB);

		expecting(
				same(contactQuery.map(Contact.TO_PARTY), partyB));
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
		
		expecting(
				values(sneerA.findContact(partyB).flatMap(Contact.TO_NICKNAME), "Party Boy"));

		ReplaySubject<String> nicknames = ReplaySubject.create();

		sneerA.findContact(partyB).flatMap(Contact.TO_NICKNAME).subscribe(nicknames);

		sneerA.addContact("Party Man", partyB);

		expecting(
				values(nicknames, "Party Boy", "Party Man"),
				values(sneerA.findContact(partyB).flatMap(Contact.TO_NICKNAME), "Party Man"));
	}
	
	@Test
	public void contactListSequence() throws FriendlyException {

		Party partyB = sneerA.produceParty(userB);
		Party partyC = sneerA.produceParty(userC);
		
		expecting(
				values(sneerA.contacts()));
		
		sneerA.addContact("Party Boy", partyB);
		
		expecting(
				Observable.zip(sneerA.contacts(), sneerA.findContact(partyB), new Func2<List<Contact>, Contact, Void>() {  @Override public Void call(List<Contact> t1, Contact t2) {
					assertArrayEquals(new Contact[]{t2}, t1.toArray());
					return null;
				} }));
		
		
		sneerA.addContact("Party Boy", partyC);

		expecting(
				Observable.zip(sneerA.contacts(), sneerA.findContact(partyB), sneerA.findContact(partyC), new Func3<List<Contact>, Contact, Contact, Void>() {  @Override public Void call(List<Contact> t1, Contact t2, Contact t3) {
					assertArrayEquals(new Contact[]{t2, t3}, t1.toArray());
					return null;
				} }));
	}
	
	
	@Test
	public void preferredNickname() {
		
		Profile profileBFromB = sneerB.profileFor(sneerB.self());
		Profile profileBFromA = sneerA.profileFor(sneerA.produceParty(userB));
		
		profileBFromB.setPreferredNickname("Party Boy");
		
		assertEqualsUntilNow(profileBFromA.preferredNickname(), "Party Boy");
		
		profileBFromB.setPreferredNickname("Party Man");

		assertEqualsUntilNow(profileBFromA.preferredNickname(), "Party Man");
		
	}

}
