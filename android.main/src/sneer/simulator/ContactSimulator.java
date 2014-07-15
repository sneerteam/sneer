package sneer.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;

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

	
	@Override public Observable<String> nickname() { return nickname; }
	@Override public void setNickname(String newNickname) { nickname.onNext(newNickname); }

}
