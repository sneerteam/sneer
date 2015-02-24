package sneer.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

import java.util.Collection;

import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import sneer.Conversation;
import sneer.android.ui.MainActivity;
import sneer.commons.Clock;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.Exceptions;

import static sneer.android.SneerAndroidSingleton.sneer;

public class Notifier {

	private static final int NOTIFICATION_ID = 1;

	private static Context context;
	private static Handler handler;
	private static NotificationManager notificationManager;
	private static CompositeSubscription subscription;


	public static void start(Context context) {
		Notifier.context = context;
		handler = new Handler(context.getMainLooper());
		notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		resume();
	}


	public static void resume() {
		Exceptions.check(context != null);

		new AsyncTask<Void, Void, Void>() { @Override protected Void doInBackground(Void[] ignored) {
			doResume(); return null;
		}}.execute();
	}


	public static void pause() {
		new AsyncTask<Void, Void, Void>() { @Override protected Void doInBackground(Void[] ignored) {
			doPause(); return null;
		}}.execute();
	}


	private static void doResume() {
		if (isSubscribed()) return;

		final CompositeSubscription currentSub = new CompositeSubscription();
		subscription = currentSub;

		currentSub.add(sneer().conversations().subscribe(new Action1<Collection<Conversation>>() {
			@Override
			public void call(Collection<Conversation> conversations) {
				for (Conversation c : conversations)
					subscribeToUnreadMessageCount(c, currentSub);
			}
		}));
	}


	private static void subscribeToUnreadMessageCount(Conversation c, CompositeSubscription currentSub) {
		currentSub.add(
			c.unreadMessageCount().subscribe(new Action1<Long>() { @Override public void call(final Long unreadMessageCount) {
				if (unreadMessageCount > 0) handler.post(new Runnable() { public void run() {
					createNotification();
				}});
			}})
		);
	}


	private static void doPause() {
		notificationManager.cancel(NOTIFICATION_ID);
		if (isUnsubscribed()) return;

		subscription.unsubscribe();
		subscription = null;
	}


	private static boolean isSubscribed() {
		if (subscription != null) {
			SystemReport.updateReport("notifications/redundant-subscribe");
			return true;
		}
		return false;
	}


	private static boolean isUnsubscribed() {
		if (subscription == null) {
			SystemReport.updateReport("notifications/redundant-unsubscribe");
			return true;
		}
		return false;
	}


	private static void createNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

		builder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("New Messages")
				//.setContentText("You have new messages")
				.setWhen(Clock.now())
				.setAutoCancel(true)
				.setContentIntent(pendingIntent)
				.setOngoing(false);

		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}

}
