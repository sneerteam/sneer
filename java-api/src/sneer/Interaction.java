package sneer;

import java.util.*;

import rx.Observable;
import sneer.commons.*;
import sneer.rx.*;

public interface Interaction {

	Party party();
		
	Observable<List<InteractionEvent>> events();
	Observed<Long> mostRecentEventTimestamp();
	Observed<String> mostRecentEventContent();
	
	/** Publish a new message with isOwn() true, with party() as the audience and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String content);

	Observed<List<InteractionMenuItem>> menu();
	
	
	Comparator<Interaction> MOST_RECENT_FIRST = new Comparator<Interaction>() {  @Override public int compare(Interaction i1, Interaction i2) {
		return Comparators.compare(i1.mostRecentEventTimestamp().mostRecent(), i2.mostRecentEventTimestamp().mostRecent());
	}};
	
}
