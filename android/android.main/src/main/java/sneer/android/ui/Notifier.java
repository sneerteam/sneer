package sneer.android.ui;

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

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.commons.Clock;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.Exceptions;
import sneer.convos.Notifications;
import sneer.main.R;

import static sneer.android.SneerAndroidContainer.component;

public class Notifier {

	private static final String TAG = "UNREAD CONVOS";
	private static final int NOTIFICATION_ID = 1;

	private static Context context;
	private static Handler handler;
	private static NotificationManager notificationManager;
	private static Subscription subscription;

	public static void start(Context context) {
		Notifier.context = context;
		handler = new Handler(context.getMainLooper());
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		resume();
	}

	public static void resume() {
		Exceptions.check(context != null);

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void[] ignored) {
				doResume();
				return null;
			}
		}.execute();
	}

	public static void pause() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void[] ignored) {
				doPause();
				return null;
			}
		}.execute();
	}

	private static void doResume() {
		if (isSubscribed()) return;

		subscription = notifications().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Notifications.Notification>() {
			@Override
			public void call(Notifications.Notification notification) {
				refresh(notification);
			}
		});
	}

	private static Observable<Notifications.Notification> notifications() {
		return component(Notifications.class).get();
	}

	private static void refresh(Notifications.Notification notification) {
		if (notification == null) {
			cancelNotification();
			return;
		}
		Long convoId = notification.convoId;
		Intent intent = convoId != null
				? convoActivityIntent(convoId)
				: convosActivityIntent();

		notify(notification, intent);
	}

	private static Intent convosActivityIntent() {
		Intent intent = new Intent();
		intent.setClass(context, ConvosActivity.class);
		return intent;
	}

	private static Intent convoActivityIntent(Long convoId) {
		Intent intent = new Intent();
		intent.setClass(context, ConvoActivity.class);
		intent.putExtra("id", convoId);
		return intent;
	}

	private static void notify(Notifications.Notification notification, Intent intent) {
		notify(intent, notification.title, notification.subText, notification.text);
	}

	private static void doPause() {
		if (isUnsubscribed()) return;

		unsubscribe();
		cancelNotification();
	}

	private static void cancelNotification() {
		handler.post(new Runnable() { public void run() {
			notificationManager.cancel(TAG, NOTIFICATION_ID);
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
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
		notificationManager.notify(TAG, NOTIFICATION_ID, builder.build());
	}

}
