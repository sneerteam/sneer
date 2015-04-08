package sneer;

import java.util.List;

import rx.Observable;

public interface Conversations {

	/** All Conversations you have had, ordered by most recent first. */
	Observable<List<Conversation>> all();

	/** @return an existing Conversation with party or a new one if it doesn't exist. */
	Conversation withParty(Party party);

	/** @return an existing Conversation with party or a new one if it doesn't exist. */
	Conversation withContact(Contact contact);

	/** All Conversations of messageType have had, ordered by most recent first. */
	Observable<List<Conversation>> ofType(String messageType);

	void setMenuItems(List<ConversationMenuItem> menuItems);

	Observable<Notification> notifications();
	/** Ignores conversation when emitting notifications for unread messages. */
	void notificationsStartIgnoring(Conversation conversation);
	void notificationsStopIgnoring();

	interface Notification {
		/** Zero or more conversations related to this notification */
		List<Conversation> conversations();

		String title();

		String text();

		String subText();
	}

}
