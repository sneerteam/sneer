package sims.sneer.convos;

import rx.Observable;
import sneer.convos.Notifications;

public class NotificationsSim implements Notifications {

	private static final String TAG = NotificationsSim.class.getSimpleName();
	private static Notification notifications;

	@Override
	public Observable<Notification> get() {
		return Observable.just(notifications);
	}

	public static void turnNotificationsOn() {
		long currentTimeMillis = System.currentTimeMillis();
		notifications = buildNotification(currentTimeMillis, "Title " + currentTimeMillis, "Text " + currentTimeMillis, "SubText " + currentTimeMillis);
	}

	public static void turnNotificationsOff() {
		notifications = null;
	}

	private static Notification buildNotification(final Long convoId, final String title, final String text, final String subText) {
		return new Notification(convoId, title, text, subText);
	}
}
