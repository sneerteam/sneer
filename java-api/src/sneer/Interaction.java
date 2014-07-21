package sneer;

import java.util.*;

import rx.Observable;
import sneer.commons.*;

public interface Interaction {

	Party party();
		
	Observable<InteractionEvent> events();
	long mostRecentEventTimestamp();
	
	/** Publish a new message with isOwn() true, with party() as the audience, with the received content and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String content);

	
	Comparator<Interaction> MOST_RECENT_FIRST = new Comparator<Interaction>() {  @Override public int compare(Interaction i1, Interaction i2) {
		return Comparators.compare(i1.mostRecentEventTimestamp(), i2.mostRecentEventTimestamp());
	}};
	
}
