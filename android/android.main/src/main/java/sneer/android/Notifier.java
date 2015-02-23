package sneer.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;

import java.util.Collection;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.android.ui.MainActivity;
import sneer.commons.Clock;

import static sneer.android.SneerAndroidSingleton.sneer;

public class Notifier {

	private static final int NOTIFICATION_ID = 1;
	private static NotificationManager notificationManager = null;
	static Handler handler = new Handler(Looper.getMainLooper());
	private static Context context;
	private static boolean isPaused;

	private static void notifyNewMessagesInBackground(Context context) {
		Notifier.context = context;
		handler.post(new Runnable() { @Override public void run() {
			notifyNewMessages();
		}});
	}


	private static void notifyNewMessages() {
		sneer().conversations().subscribe(new Action1<Collection<Conversation>>() { @Override public void call(Collection<Conversation> conversations) {
			for (final Conversation c : conversations) {
				c.unreadMessageCount().subscribe(new Action1<Long>() { @Override public void call(final Long unreadMessageCount) {
					if (unreadMessageCount > 0)
						handler.post(new Runnable() { public void run() {
							createNotificationIfNecessary();
						}});
				}});
			}
		}});
	}


	private static void createNotificationIfNecessary() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

		String contentTitle = "New Messages";
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(contentTitle)
				.setContentText("You have new messages")
				.setWhen(Clock.now())
				.setAutoCancel(true)
				.setContentIntent(pendingIntent)
				.setOngoing(false);

		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}

	synchronized
	public static void pause() {
		isPaused = true;
		if (notificationManager != null)
			notificationManager.cancel(NOTIFICATION_ID);
	}

	synchronized
	public static void resume() {
		isPaused = false;
		notifyNewMessagesInBackground(context);
	}

}
