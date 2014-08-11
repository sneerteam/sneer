package sneer;

import java.util.*;

import sneer.rx.*;

public interface Contact {

	Party party();
	
	Observed<String> nickname();

	/** @return null if the new nickname is ok or a reason why the new nickname is not ok. */
	String problemWithNewNickname(String newNick);
	
	Comparator<Contact> BY_NICKNAME = new Comparator<Contact>() { @Override public int compare(Contact c1, Contact c2) {
		return c1.nickname().mostRecent().compareToIgnoreCase(c2.nickname().mostRecent());
	}};

	
}
