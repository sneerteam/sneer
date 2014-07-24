package sneer;

import rx.*;

public interface Session<T> {
	
	Contact contact();

	void send(T value);

	Observable<T> received();
	
	void dispose();
}
