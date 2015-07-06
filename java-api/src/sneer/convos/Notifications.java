package sneer.convos;

import rx.Observable;

public interface Notifications {

	/** Emits null when there's no notification */
	Observable<Notification> get();

	/** Ignores convo when emitting notifications for unread messages. */
	void startIgnoring(Long convoId);
	void stopIgnoring();

	interface Notification {
		/** Null if more than one convo */
		Long convoId();

		String title();

		String text();

		String subText();
	}

}
