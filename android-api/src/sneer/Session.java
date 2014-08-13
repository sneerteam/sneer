package sneer;

import rx.*;
import sneer.rx.*;

public interface Session {
	
	Observed<String> contactNickname();

	void send(Object value);

	Observable<Object> received();
	
	void dispose();
}
