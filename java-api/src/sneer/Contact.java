package sneer;

import rx.Observable;
import rx.functions.Func1;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.Observed;

public interface Contact {

	/** @return Emitted value is never null. */
	Observed<String> nickname();
	void setNickname(String newNick) throws FriendlyException;

	//INVITES-TODO: Change this to Observed<Party>.
	/** @return Emitted value may be null. */
	Party party();
	//INVITES-TODO: Add this:
	/** @throws FriendlyException if this Contact's party is already set or if party is already set for another Contact. */
	void setParty(Party party) throws FriendlyException;

	//INVITES-TODO: Add this: (generated when Sneer.produceContact is called with a null party)
	/** @return A code that can be sent to the party adding us that will cause Sneer to add that party back automatically as this Contact's party (null if this Contact's party is already set). */
	String inviteCode();


	Func1<Contact, rx.Observable<String>> TO_NICKNAME = new Func1<Contact, rx.Observable<String>>() { @Override public Observable<String> call(Contact contact) {
		return contact.nickname().observable();
	}};

	Func1<Contact, Party> TO_PARTY = new Func1<Contact, Party>() { @Override public Party call(Contact contact) {
		return contact.party();
	}};

}
