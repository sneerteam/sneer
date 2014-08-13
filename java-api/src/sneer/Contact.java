package sneer;

import java.util.*;

import sneer.rx.*;

public interface Contact {

	Party party();
	
	Observed<String> nickname();
	
	Comparator<Contact> BY_NICKNAME = new Comparator<Contact>() { @Override public int compare(Contact c1, Contact c2) {
		return c1.nickname().current().compareToIgnoreCase(c2.nickname().current());
	}};

	
}
