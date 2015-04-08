package sneer;

import rx.Observable;
import rx.functions.Func1;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.Observed;

public interface Contact {

	/** @return Emitted value is never null. */
	Observed<String> nickname();
	void setNickname(String newNick) throws FriendlyException;

	/** @return Emitted value may be null. */
	Observed<Party> party();

	/** @throws FriendlyException if this Contact's party is already set or if party is already set for another Contact. */
	void setParty(Party party) throws FriendlyException;

	/** @return A code that can be sent to the party adding us that will cause Sneer to add that party back automatically as this Contact's party (null if this Contact's party is already set). */
	String inviteCode();

	Func1<Contact, rx.Observable<String>> TO_NICKNAME = new Func1<Contact, rx.Observable<String>>() { @Override public Observable<String> call(Contact contact) {
		return contact.nickname().observable();
	}};

	Func1<Contact, Party> TO_PARTY = new Func1<Contact, Party>() { @Override public Party call(Contact contact) {
		return contact.party().current();
	}};

}
