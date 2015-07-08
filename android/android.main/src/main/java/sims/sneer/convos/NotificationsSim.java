package sims.sneer.convos;

import android.util.Log;

import rx.Observable;
import sneer.convos.Notifications;

public class NotificationsSim implements Notifications {

	private static Notification notifications;

	@Override
	public Observable<Notification> get() {
		Log.i("NOTIFTEST", "get->called");
		return Observable.just(notifications);
	}

	@Override
	public void startIgnoring(Long convoId) {
		Log.i("NOTIFTEST", "startIgnoring->" + convoId);
	}

	@Override
	public void stopIgnoring() {
		Log.i("NOTIFTEST", "stopIgnoring->called");
	}

	public static void turnNotificationsOn() {
		long currentTimeMillis = System.currentTimeMillis();
		notifications = buildNotification(currentTimeMillis, "Title " + currentTimeMillis, "Text " + currentTimeMillis, "SubText " + currentTimeMillis);
	}

	public static void turnNotificationsOff() {
		notifications = null;
	}

	private static Notification buildNotification(final Long convoId, final String title, final String text, final String subText) {
		return new Notification() {
			@Override
			public Long convoId() {
				return convoId;
			}

			@Override
			public String title() {
				return title;
			}

			@Override
			public String text() {
				return text;
			}

			@Override
			public String subText() {
				return subText;
			}
		};
	}
}
