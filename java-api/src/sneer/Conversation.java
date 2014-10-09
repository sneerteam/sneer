package sneer;

import java.util.*;

import rx.Observable;
import sneer.commons.*;
import sneer.rx.*;

public interface Conversation {

	Party party();
		
	Observable<List<Message>> messages();
	Observed<Long> mostRecentMessageTimestamp();
	Observed<String> mostRecentMessageContent();
	
	/** Publish a new message with isOwn() true, with party() as the audience and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String content);

	Observable<List<ConversationMenuItem>> menu();
	
	Observable<Long> unreadMessageCount();
	void setBeingRead(boolean isBeingRead);
	
	Comparator<Conversation> MOST_RECENT_FIRST = new Comparator<Conversation>() {  @Override public int compare(Conversation i1, Conversation i2) {
		return Comparators.compare(i1.mostRecentMessageTimestamp().current(), i2.mostRecentMessageTimestamp().current());
	}};
	
}
