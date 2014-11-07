package sneer;

import rx.Observable;
import rx.functions.Func1;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.Observed;

public interface Contact {

	Party party();

	Observed<String> nickname();

	void setNickname(String newNick) throws FriendlyException;


	Func1<Contact, rx.Observable<String>> TO_NICKNAME = new Func1<Contact, rx.Observable<String>>() {  @Override public Observable<String> call(Contact contact) {
		return contact.nickname().observable();
	} };

	Func1<Contact, Party> TO_PARTY = new Func1<Contact, Party>() {  @Override public Party call(Contact contact) {
		return contact.party();
	} };

}
