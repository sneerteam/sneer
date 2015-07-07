package sims.sneer.convos;

import android.util.Log;

import rx.Observable;
import sneer.convos.Notifications;

public class NotificationsSim implements Notifications {

	@Override
	public Observable<Notification> get() {
		Log.i("NOTIFTEST", "get->called");
		return Observable.just(null);
	}

	@Override
	public void startIgnoring(Long convoId) {
		Log.i("NOTIFTEST", "startIgnoring->" + convoId);
	}

	@Override
	public void stopIgnoring() {
		Log.i("NOTIFTEST", "stopIgnoring->called");
	}

}
