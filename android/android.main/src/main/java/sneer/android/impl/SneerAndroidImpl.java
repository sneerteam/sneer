package sneer.android.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.R;
import sneer.android.SneerAndroid;
import sneer.android.database.SneerSqliteDatabase;
import sneer.android.ipc.PluginHandler;
import sneer.android.ipc.PluginManager;
import sneer.android.ui.ConversationActivity;
import sneer.android.utils.AndroidUtils;
import sneer.android.utils.LogUtils;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.Tuple;

import java.io.File;
import java.io.IOException;

import static sneer.android.ui.ConversationActivity.PARTY_PUK;

public class SneerAndroidImpl implements SneerAndroid {

	private SneerAdmin sneerAdmin;
	private static String error;
	public static AlertDialog errorDialog;
	private PluginManager pluginManager;

	public SneerAndroidImpl(Context context) {
		try {
			init(context);
		 } catch (FriendlyException e) {
			error = e.getMessage();
		}
	}


	private void init(Context context) throws FriendlyException {
		sneerAdmin = newSneerAdmin(context);
        System.out.println("YOUR PUBLIC KEY IS: " + sneerAdmin.privateKey().publicKey().toHex());

		pluginManager = new PluginManager(context, sneer());
		pluginManager.initPlugins();

//		initNotifications(context);
	}


	private void initNotifications(final Context context) {
		sneer().tupleSpace().filter()
			.audience(sneer().self().publicKey().current())
			.type("message")
			.tuples()
			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple tuple) {
				return !tuple.author().equals(sneer().self().publicKey().current());
			}})
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple tuple) {

				log("-------------> "+ tuple.type() + " - " + tuple.payload());

				PluginHandler plugin = null;
				Intent intent;
				if ("chat".equals(tuple.get("message-type"))) {
					intent = new Intent(context, ConversationActivity.class);
					intent.putExtra(PARTY_PUK, tuple.author());
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				} else {
					plugin = pluginManager.tupleViewer((String)tuple.get("message-type"));
					if (plugin == null) {
						// TODO intent should direct to app store if plugin not installed
						return;
					}
					intent = plugin.resume(tuple);
				}

				log("-------------> " + tuple.type() + ": " + intent.getExtras().getParcelable(IPCProtocol.RESULT_RECEIVER));

//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

				notifyUser(context, tuple, plugin == null ? tuple.type() : plugin.notificationLabel(), PendingIntent.getActivity(context, 0, intent, 0));
			}});
	}


	private static void notifyUser(Context context, Tuple tuple, String notificationLabel, PendingIntent pendIntent) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(R.drawable.ic_launcher)
			.setContentText("" + ("chat".equals(tuple.get("message-type")) ? tuple.payload() : (tuple.get("label") == null ? tuple.payload() : tuple.get("label"))))
			.setContentTitle(notificationLabel)
			.setWhen(tuple.timestamp())
			.setAutoCancel(true)
			.setOngoing(false)
			.setContentIntent(pendIntent);

//		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//		mNotificationManager.notify("sneer:"+tuple.type(), 0, builder.getNotification());
	}


	private SneerAdmin newSneerAdmin(Context context) throws FriendlyException {

		File adminDir = new File(context.getFilesDir(), "admin");
		adminDir.mkdirs();
		File secureFile = new File(adminDir, "tupleSpace.sqlite");
		try {
			SneerSqliteDatabase db = SneerSqliteDatabase.openDatabase(secureFile);
			SneerAdmin admin = newSneerAdmin(db);
			return admin;
		} catch (IOException e) {
			SystemReport.updateReport("Error starting Sneer", e);
			throw new FriendlyException("Error starting Sneer", e);
		}
	}


	private static SneerAdmin newSneerAdmin(SneerSqliteDatabase db) {
		try {
			return (SneerAdmin) sneerAdminFactory().getMethod("create", Object.class).invoke(null, db);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	private static Class<?> sneerAdminFactory() {
		try {
			return Class.forName("sneer.admin.SneerAdminFactory");
		} catch (ClassNotFoundException e) {
			return null;
		}
	}


	public static boolean isCoreAvailable() {
		return sneerAdminFactory() != null;
	}


	@Override
	public SneerAdmin admin() {
		return sneerAdmin;
	}


	@Override
	public boolean checkOnCreate(Activity activity) {
		if (error == null) return true;
		AndroidUtils.finishWith(error, activity);
		return false;
	}


	@Override
	public Sneer sneer() {
		return admin().sneer();
	}


	@Override
	public boolean isClickable(Message message) {
		return pluginManager.isClickable(message);
	}


	@Override
	public void doOnClick(Message message) {
		pluginManager.doOnClick(message);
	}


	private void log(String log) {
		LogUtils.info(SneerAndroidImpl.class, log);
	}

}
