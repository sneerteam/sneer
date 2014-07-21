package sneer.impl.simulator;

import sneer.*;
import sneer.rx.*;

public class ContactSimulator implements Contact {

	private final Party party;
	private final ObservedSubject<String> nickname;

	
	ContactSimulator(String nickname, Party party) {
		this.nickname = ObservedSubject.create(nickname);
		this.party = party;
	}


	@Override
	public Party party() {
		return party;
	}


	@Override
	public Observed<String> nickname() {
		return nickname.observed();
	}


//	@Override public Observable<String> nickname() { return nickname; }
//	@Override public void setNickname(String newNickname) { nickname.onNext(newNickname); }
//	
//	public int compareTo(Contact contact) {
//		return this.nickname().compareToIgnoreCase(((Contact) contact).nickname());
//	}	
// 
//	public static Comparator<Contact> BY_NICKNAME_IGNORING_CASE = new Comparator<Contact>() {
// 
//	    public int compare(Contact nick1, Contact nick2) {
//	      return nick1.compareTo(nick2);
//	    } 
//	};

}
