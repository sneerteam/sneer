package sneer;

import rx.Observable;

public interface Interaction {

	Contact contact();
	
	Contact contact(Party party);
	
	Observable<InteractionEvent> interactionEvents();
	
	/** Publish a new InteractionEvent with isOwn() true, with contact() as the audience, with the received content and using System.currentTimeMillis() as the timestamp. */
	void sendInteractionEvent(String content);
	
	long lastInteractionEventTimestamp();
	
}
