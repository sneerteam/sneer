package sneer;

import rx.*;
import rx.functions.*;
import sneer.rx.*;

public interface Contact {

	Party party();
	
	Observed<String> nickname();
	
	
	Func1<Contact, rx.Observable<String>> TO_NICKNAME = new Func1<Contact, rx.Observable<String>>() {  @Override public Observable<String> call(Contact t1) {
		return t1.nickname().observable();
	} };
	
	Func1<Contact, Party> TO_PARTY = new Func1<Contact, Party>() {  @Override public Party call(Contact t1) {
		return t1.party();
	} };
	
}
