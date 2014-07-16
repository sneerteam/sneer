package sneer.simulator;

import java.util.*;

import rx.Observable;
import rx.subjects.*;
import sneer.*;
import sneer.rx.*;

public class ContactSimulator implements Contact {

	private final Party party;
	private final Subject<String, String> nickname;

	
	ContactSimulator(String nickname, String partyName) {
		this(nickname, new PartySimulator(partyName));
	}
	
	
	public ContactSimulator(String nickname, Party party) {
		this.nickname = BehaviorSubject.create(nickname);
		this.party = party;
	}


	@Override
	public Party party() {
		return party;
	}


	@Override
	public Observed<String> nickname() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setNickname(String newNickname) {
		// TODO Auto-generated method stub
		
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
