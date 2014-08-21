package sneer;

import rx.*;

public interface Session {
	
	Party peer();

	void sendMessage(Object value);
	Observable<Object> receivedMessages();
	
	void dispose();
}
