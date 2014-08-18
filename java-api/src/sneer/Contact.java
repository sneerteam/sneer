package sneer;

import rx.*;
import rx.functions.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;

public interface Contact {

	Party party();
	
	Observed<String> nickname();
	
	/** @return null if the new nickname is ok or a reason why the new nickname is not ok. */
	String problemWithNewNickname(String newNick);

	void setNickname(String newNick) throws FriendlyException;


	Func1<Contact, rx.Observable<String>> TO_NICKNAME = new Func1<Contact, rx.Observable<String>>() {  @Override public Observable<String> call(Contact t1) {
		return t1.nickname().observable();
	} };
	
	Func1<Contact, Party> TO_PARTY = new Func1<Contact, Party>() {  @Override public Party call(Contact t1) {
		return t1.party();
	} };
	
}
