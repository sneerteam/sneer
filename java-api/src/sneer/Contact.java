package sneer;

import sneer.rx.*;

public interface Contact {

	Party party();
	
	Observed<String> nickname();
	void setNickname(String newNickname);
	
}
