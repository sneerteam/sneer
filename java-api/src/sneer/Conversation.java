package sneer;

import java.util.List;

import rx.Observable;
import sneer.rx.Observed;

public interface Conversation {

	/** @return The Contact having this conversation with us. */
	Contact contact();

	/** @return Emits false when messages cannot be sent (contact() doesn't have a party yet because it is an open invite). */
	Observable<Boolean> canSendMessages();
	void sendMessage(String label);

	Observable<Session> startSession(String type);

	Observable<List<Session>> sessions(); //TODO: remove this
	Observable<List<ConversationItem>> items(); //Item: Message or Session
	Observable<String> mostRecentMessageContent();
	Observable<Long> mostRecentMessageTimestamp();

	Observable<List<ConversationItem>> unreadMessages();
	Observable<Long> unreadMessageCount();
	void setRead(ConversationItem last);

	Observable<List<ConversationMenuItem>> menu();

}
