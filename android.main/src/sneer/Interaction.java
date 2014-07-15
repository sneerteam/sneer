package sneer;

import rx.Observable;

public interface Interaction {

	Party party();
		
	Observable<InteractionEvent> events();
	long mostRecentEventTimestamp();
	
	/** Publish a new message with isOwn() true, with party() as the audience, with the received content and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String content);
	
}
