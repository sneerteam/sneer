package sneer;

import rx.*;

public interface Contact {

	Party party();
	
	Observable<String> nickname();
	void setNickname(String newNickname);
	
}
