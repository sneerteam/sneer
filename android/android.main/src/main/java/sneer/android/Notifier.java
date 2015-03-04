package sneer.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.Conversation;
import sneer.Conversations;
import sneer.PublicKey;
import sneer.android.ui.ConversationActivity;
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
	private static Subscription subscription;

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

		subscription = notifications().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Conversations.Notification>() { @Override public void call(Conversations.Notification notification) {
			refresh(notification);
		}});
	}

	private static Observable<Conversations.Notification> notifications() {
		return sneer().conversations().notifications();
	}

	private static void refresh(Conversations.Notification notification) {
		List<Conversation> conversations = notification.conversations();
		if (conversations.size() == 0) {
			cancelNotification();
			return;
		}
		Intent intent = conversations.size() == 1
				? conversationActivityIntent(conversations.get(0))
				: mainActivityIntent();
		notify(notification, intent);
	}

	private static Intent mainActivityIntent() {
		Intent intent = new Intent();
		intent.setClass(context, MainActivity.class);
		return intent;
	}

	private static Intent conversationActivityIntent(Conversation conversation) {
		Intent intent = new Intent();
		intent.setClass(context, ConversationActivity.class);
		intent.putExtra("partyPuk", partyPuk(conversation));
		return intent;
	}

	private static PublicKey partyPuk(Conversation conversation) {
		return conversation.party().publicKey().current();
	}

	private static void notify(Conversations.Notification notification, Intent intent) {
		notify(intent, notification.title(), notification.subText(), notification.text());
	}

	private static void doPause() {
		if (isUnsubscribed()) return;

		unsubscribe();
		cancelNotification();
	}

    private static void cancelNotification() {
        handler.post(new Runnable() { public void run() {
            notificationManager.cancel(NOTIFICATION_ID);
        }});
    }

	private static void unsubscribe() {
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

	private static void notify(Intent intent, String title, String contentInfo, String contentText) {
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_large))
				.setContentTitle(title)
				.setTicker(contentText)
				.setContentText(contentText)
				.setSubText(contentInfo)
				.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
				.setVibrate(new long[]{0, 200, 200, 200, 0, -1})
				.setLights(Color.MAGENTA, 1, 1)
				.setOnlyAlertOnce(true)
				.setWhen(Clock.now())
				.setAutoCancel(true)
				.setContentIntent(pendingIntent)
				.setOngoing(false);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}

}
