package sims.sneer.convos;

import android.util.Log;

import rx.Observable;
import sneer.convos.Notifications;

public class NotificationsSim implements Notifications {

	@Override
	public Observable<Notification> get() {
		Notification n = buildNotification(1043L, "Test title", "Test text", "Test subText");
		return Observable.just(n);
	}

	@Override
	public void startIgnoring(Long convoId) {
		Log.i("NOTIFTEST", "startIgnoring->" + convoId);
	}

	@Override
	public void stopIgnoring() {
		Log.i("NOTIFTEST", "stopIgnoring->called");
	}

	private Notifications.Notification buildNotification(final Long convoId, final String title, final String text, final String subText) {
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
