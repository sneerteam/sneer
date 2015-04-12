package sneer;

import java.util.List;

import rx.Observable;
import sneer.rx.Observed;

public interface Conversation {

//	/** @return The Contact having this conversation with us. */
	Contact contact();

// INVITES-TODO: Add this: For now, the UI should disable message sending based on this. We cannot reify two different instances of Conversation. It has to be the same instance, reacting according to it's contact having a party or not (open invite).
//  /** @return Emits false when messages cannot be sent (contact() doesn't have a party yet because it is an open invite). */
	Observable<Boolean> canSendMessages();
	void sendMessage(String label);

	Observable<List<Message>> messages();
	Observable<String> mostRecentMessageContent();
	Observable<Long> mostRecentMessageTimestamp();

	Observable<List<Message>> unreadMessages();
	Observable<Long> unreadMessageCount();

	void setRead(Message last);

	Observable<List<ConversationMenuItem>> menu();

}