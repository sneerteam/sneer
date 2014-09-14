package sneer.android.main;

import static sneer.SneerAndroid.OWN_PRIK;
import static sneer.SneerAndroid.RESULT_RECEIVER;
import static sneer.SneerAndroid.SESSION_ID;
import static sneer.android.main.SneerPluginInfo.InteractionType.MESSAGE_COMPOSE;
import static sneer.android.main.SneerPluginInfo.InteractionType.SESSION;
import static sneer.commons.exceptions.Exceptions.check;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.ConversationMenuItem;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.main.core.SneerSqliteDatabase;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.impl.simulator.SneerAdminSimulator;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SneerApp extends Application {
	
	Func1<List<SneerPluginInfo>, Observable<List<ConversationMenuItem>>> fromSneerAppInfoList = new Func1<List<SneerPluginInfo>, Observable<List<ConversationMenuItem>>>() {  @Override public Observable<List<ConversationMenuItem>> call(List<SneerPluginInfo> apps) {
		return Observable.from(apps)
			.filter(new Func1<SneerPluginInfo, Boolean>() {  @Override public Boolean call(SneerPluginInfo t1) {
				return t1.canCompose();
			} })
			.map(new Func1<SneerPluginInfo, ConversationMenuItem>() { @Override public ConversationMenuItem call(final SneerPluginInfo app) {
				return new ConversationMenuItemImpl(app);
			} })
			.toList();
	} };

	private final class ConversationMenuItemImpl implements ConversationMenuItem {
		
		private final SneerPluginInfo app;

		private ConversationMenuItemImpl(SneerPluginInfo app) {
			this.app = app;
		}

		@Override
		public void call(PublicKey partyPuk) {
			startPlugin(app, partyPuk);
		}

		@Override
		public byte[] icon() {
			try {
				Drawable icon = resourceForPackage(app.packageName).getDrawable(app.menuIcon);
				return bitmapFor(icon);
			} catch (Exception e) {
				Log.w(SneerApp.class.getSimpleName(), "Error loading bitmap", e);
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public String caption() {
			return app.menuCaption;
		}
	}


	private static AlertDialog errorDialog;

	private static SneerAdmin ADMIN;

	private static Context context;

	private static String error;
	
	@Override
	public void onCreate() {

		context = getApplicationContext();

		SneerPluginInfo.initialDiscovery(context);
		
		try {
			initialize();
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
		
		SneerPluginInfo.plugins()
			.flatMap(fromSneerAppInfoList)
			.subscribe(new Action1<List<ConversationMenuItem>>() {  @Override public void call(List<ConversationMenuItem> menuItems) {
				sneer().setConversationMenuItems(menuItems);
			} });
		
		TupleSpaceService.startTupleSpaceService(context);
		
		super.onCreate();
	}
	
	private AtomicLong nextSessionId = new AtomicLong(0);
	
	private void startPlugin(SneerPluginInfo app, PublicKey peer) {
		if (app.interactionType == SESSION) startSession(app, peer);
		if (app.interactionType == MESSAGE_COMPOSE) startMessage(app, peer);
	}


	private void startMessage(final SneerPluginInfo app, final PublicKey peer) {
		Intent intent = new Intent();
		intent.setClassName(app.packageName, app.activityName);
		
		final ClassLoader classLoader = getClassLoader();
		
		SharedResultReceiver result = new SharedResultReceiver(new Action1<Bundle>() {  @Override public void call(Bundle t1) {
			
			try {
				t1.setClassLoader(classLoader);
				Object[] ret = (Object[]) ((Value)t1.getParcelable("value")).get();
				info("Receiving " + ret.length + " messages type '"+app.tupleType+"' from " + app.packageName + "." + app.activityName);
				for (Object payload : ret) {
					sneer().tupleSpace().publisher()
						.type(app.tupleType)
						.audience(peer)
						.pub(payload);
				}
			} catch (final Throwable t) {
				toastOnMainThread("Error receiving message from plugin: " + t.getMessage(), Toast.LENGTH_LONG);
				Log.w(SneerApp.class.getSimpleName(), "Error receiving message from plugin", t);
			}
			
		}});
		
		intent.putExtra(RESULT_RECEIVER, result);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public static void toastOnMainThread(final String message, final int length) {
		AndroidSchedulers.mainThread().createWorker().schedule(new Action0() { @Override public void call() {
			Toast.makeText(context, message, length).show();
		} });
	}

	private void startSession(SneerPluginInfo app, PublicKey peer) {
		long sessionId = nextSessionId.getAndIncrement();
		
		sneer().tupleSpace().publisher()
			.audience(sneer().self().publicKey().current())
			.type("sneer/session")
			.field("session", sessionId)
			.field("partyPuk", peer)
			.field("sessionType", app.tupleType)
			.field("lastMessageSeen", (long)0)
			.pub();

		Intent intent = new Intent();
		intent.setClassName(app.packageName, app.activityName);
		intent.putExtra(SESSION_ID, sessionId);
		intent.putExtra(OWN_PRIK, admin().privateKey());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	
	private byte[] bitmapFor(Drawable icon) {
		Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		return stream.toByteArray();
	}


	private Resources resourceForPackage(String packageName) throws NameNotFoundException {
		return getPackageManager().getResourcesForApplication(packageName);
	}


	private void info(String msg) {
		Log.i(SneerApp.class.getSimpleName(), msg);
	}


	public static Sneer sneer() {
		return admin().sneer();
	}
	
	
	public static SneerAdmin admin() {
		if (ADMIN == null) throw new IllegalStateException("You must call the initialize method before you call this method.");
		return ADMIN;
	}

	
	public static void initialize() throws FriendlyException {
		check(ADMIN == null);

		ADMIN = isCoreAvailable()
			? initialize(context)
			: simulator();
	}

	
	private static boolean isCoreAvailable() {
		return sneerAdminFactory() != null;
	}

	
	private static Class<?> sneerAdminFactory() {
		try {
			return Class.forName("sneer.admin.SneerAdminFactory");
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	
	private static SneerAdmin simulator() {
		SneerAdminSimulator ret = new SneerAdminSimulator();
		setOwnName(ret.sneer(), "Neide da Silva"); //Comment this line to get an empty name.
		return ret;
	}


	private static void setOwnName(Sneer sneer, String name) {
		sneer.profileFor(sneer.self()).setOwnName(name);
	}

	
	private static SneerAdmin initialize(Context context) throws FriendlyException {

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

	
	public static boolean checkOnCreate(Activity activity) {
		if (error == null) return true;
		finishWith(error, activity);
		return false;
	}
	
	
	private static void finishWith(String message, final Activity activity) {
		if (errorDialog != null) {
			activity.finish();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {
			errorDialog.dismiss();
			errorDialog = null;
			activity.finish();
		}});
		errorDialog = builder.create();
		errorDialog.show();
	}

}
