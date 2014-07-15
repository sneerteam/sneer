package sneer;

import rx.Observable;

public interface Interaction {

	Party party();
	
	Party party(Party party);
	
	Observable<InteractionEvent> interactionEvents();
	
	/** Publish a new InteractionEvent with isOwn() true, with party() as the audience, with the received content and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String content);
	
	long lastMessageTimestamp();
	
}
