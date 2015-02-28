package sneer;

import java.util.List;

import rx.Observable;

public interface Conversations {

	/** All Conversations you have had, ordered by most recent first. */
	Observable<List<Conversation>> all();

	/** @return an existing Conversation with party or a new one if it doesn't exist. */
	Conversation with(Party party);

	/** All Conversations of messageType have had, ordered by most recent first. */
	Observable<List<Conversation>> ofType(String messageType);

	void setMenuItems(List<ConversationMenuItem> menuItems);

}
