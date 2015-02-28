package sneer;

import java.util.List;

import rx.Observable;

public interface Conversation {

	Party party();
		
	Observable<List<Message>> messages();
	Observable<Long> mostRecentMessageTimestamp();
	Observable<String> mostRecentMessageContent();
	
	/** Publish a new message with isOwn() true, with party() as the audience and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String label);

	Observable<List<ConversationMenuItem>> menu();
	
	Observable<Long> unreadMessageCount();

	void setRead(Message last);
}