package sneer;

import rx.*;
import sneer.rx.*;

public interface Session<T> {
	
	Observed<String> contactNickname();

	void send(T value);

	Observable<T> received();
	
	void dispose();
}
