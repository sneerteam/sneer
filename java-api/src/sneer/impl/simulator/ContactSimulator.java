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


	void setNickname(String newNickname) {
		nickname.set(newNickname);
	}


	private int counter;
	@Override
	public String problemWithNewNickname(String newNick) {
		return (counter++ % 3 == 0) ? "Not cool" : null;
	}

}
