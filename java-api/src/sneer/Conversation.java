package sneer;

import java.util.List;

import rx.Observable;
import sneer.rx.Observed;

public interface Conversation {

//	/** @return The Contact having this conversation with us. */
	Contact contact();

// INVITES-TODO: Add this: For now, the UI should disable message sending based on this. We cannot reify two different instances of Conversation. It has to be the same instance, reacting according to it's contact having a party or not (open invite).
//  /** @return Emits false when messages cannot be sent (contact() doesn't have a party yet because it is an open invite). */
//  Observed<Boolean> canSendMessages();

	/** Publish a new message with isOwn() true, with party() as the audience and using System.currentTimeMillis() as the timestamp. */
	void sendMessage(String label);

	Observable<List<Message>> messages();
	Observable<List<Message>> unreadMessages();
	Observable<Long> mostRecentMessageTimestamp();
	Observable<String> mostRecentMessageContent();
	

	Observable<List<ConversationMenuItem>> menu();
	
	Observable<Long> unreadMessageCount();

	void setRead(Message last);
}