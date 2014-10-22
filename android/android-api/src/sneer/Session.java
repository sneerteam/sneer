package sneer;

import rx.*;

public interface Session {
	
	Observable<String> peerName();

	void sendMessage(Object value);
	
	/** Emits the previous messages sent and received in this session, then completes. */
	Observable<Message> previousMessages();
	
	/** Emits the future messages when they are sent and received in this session. */
	Observable<Message> newMessages();
	
	void dispose();
	
}
