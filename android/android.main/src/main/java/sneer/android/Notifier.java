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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import sneer.Contact;
import sneer.Conversation;
import sneer.Party;
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
	private static CompositeSubscription subscription;
	private static boolean hasManyUnreadConversations = false;
	private static Party unreadConversationParty;

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
		hasManyUnreadConversations = false;
		unreadConversationParty = null;

		final CompositeSubscription currentSub = new CompositeSubscription();
		subscription = currentSub;

		currentSub.add(
				sneer().conversations().all().subscribe(
						new Action1<Collection<Conversation>>() {
							@Override
							public void call(Collection<Conversation> conversations) {
								for (Conversation c : conversations)
									subscribeToUnreadMessageCount(c, currentSub);
							}
						}));
	}

	}


	private static void subscribeToUnreadMessageCount(final Conversation c, CompositeSubscription currentSub) {
		currentSub.add(
			c.unreadMessageCount().debounce(300, TimeUnit.MILLISECONDS).subscribe(new Action1<Long>() {
				@Override
				public void call(final Long unreadMessageCount) {
					if (unreadMessageCount > 0) handler.post(new Runnable() { public void run() {
						if (unreadConversationParty == c.party()) return;
						if (unreadConversationParty == null)
							unreadConversationParty = c.party();
						else {
							hasManyUnreadConversations = true;
							unreadConversationParty = null;
						}
						createNotification(unreadMessageCount);
					}});
				}
			})
		);
	}


	private static void doPause() {
		if (isUnsubscribed()) return;

		subscription.unsubscribe();
		subscription = null;
		hasManyUnreadConversations = false;

		handler.post(new Runnable() { public void run() {
			notificationManager.cancel(NOTIFICATION_ID);
		}});
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


	private static void createNotification(final Long unreadMessageCount) {
		String contentText = "mostRecentMessageContent goes here :)";

		if (hasManyUnreadConversations) {
			Intent intent = new Intent();
			intent.setClass(context, MainActivity.class);
			notify(intent, "New messages", "", contentText);
			return;
		}

		Intent intent = new Intent();
		intent.setClass(context, ConversationActivity.class);
		intent.putExtra("partyPuk", unreadConversationParty.publicKey().current());

		Contact contact = sneer().findContact(unreadConversationParty);
		String contentInfo = unreadMessageCount.toString() + " new nessages";
		String nickname = "";
		if (contact == null)
			nickname = sneer().profileFor(unreadConversationParty).preferredNickname().toBlocking().first().toString();
		else
			nickname = contact.nickname().current().toString();

		notify(intent, nickname, contentInfo, contentText);
	}

	private static void notify(Intent intent, String title, String contentInfo, String contentText) {
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
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
