package sneer.android.main;

import static sneer.SneerAndroid.*;
import static sneer.android.main.core.SneerSqliteDatabase.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import rx.Observable;
import rx.functions.*;
import sneer.*;
import sneer.admin.*;
import sneer.android.main.core.*;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;
import sneer.impl.simulator.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;
import clojure.java.api.*;
import clojure.lang.*;

public class SneerApp extends Application {
	
	private static AlertDialog errorDialog;
	
	private static final boolean USE_SIMULATOR = true;
	
	private static SneerAdmin ADMIN = null;

	private static Context context;

	private static String error;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void onCreate() {

		context = getApplicationContext();

		SneerAppInfo.initialDiscovery(context);
		
		try {
			initialize();
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
		
		SneerAppInfo.apps()
			.flatMap(new Func1<List, Observable<List<ConversationMenuItem>>>() {  @Override public Observable<List<ConversationMenuItem>> call(List t1) {
				@SuppressWarnings("unchecked")
				List<SneerAppInfo> apps = t1;

				return Observable.from(apps)
					.map(new Func1<SneerAppInfo, ConversationMenuItem>() {  @Override public ConversationMenuItem call(final SneerAppInfo t1) {
						return new ConversationMenuItem() {
							
							@Override
							public void call(PublicKey partyPuk) {
								createSession(t1, partyPuk);
							}
							
							@Override
							public byte[] icon() {
								Drawable icon;
								try {
									icon = getPackageManager().getResourcesForApplication(t1.packageName).getDrawable(t1.icon);
									Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
									ByteArrayOutputStream stream = new ByteArrayOutputStream();
									bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
									return stream.toByteArray();
								} catch (Exception e) {
									Log.w(SneerApp.class.getSimpleName(), "Error loading bitmap", e);
									e.printStackTrace();
								}

								return null;
							}
							
							@Override
							public String caption() {
								return t1.label;
							}
						};
					} })
					.toList();
			} })
			.subscribe(new Action1<List<ConversationMenuItem>>() {  @Override public void call(List<ConversationMenuItem> menuItems) {
				sneer().setConversationMenuItems(menuItems);
			} });
		
		super.onCreate();
	}
	
	private AtomicLong nextSessionId = new AtomicLong(0);
	
	private void createSession(SneerAppInfo app, PublicKey peer) {
		
		long sessionId = nextSessionId.getAndIncrement();
		
		sneer().tupleSpace().publisher()
			.audience(sneer().self().publicKey().current())
			.type("sneer/session")
			.field("session", sessionId)
			.field("partyPuk", peer)
			.field("sessionType", app.type)
			.field("lastMessageSeen", (long)0)
			.pub();

		Intent intent = new Intent();
		intent.setClassName(app.packageName, app.activityName);
		intent.putExtra(SESSION_ID, sessionId);
		intent.putExtra(OWN_PRIK, admin().privateKey());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	
	public static Sneer sneer() {
		return admin().sneer();
	}
	
	
	public static SneerAdmin admin() {
		if (ADMIN == null) throw new IllegalStateException("You must call the initialize method before you call this method.");
		return ADMIN;
	}

	
	public static void initialize() throws FriendlyException {
		if (ADMIN != null) throw new FriendlyException("Sneer is being initialized more than once.");

		ADMIN = USE_SIMULATOR
			? simulator()
			: initialize(context);
	}

	
	private static SneerAdmin simulator() {
		SneerAdminSimulator ret = new SneerAdminSimulator();
		setOwnName(ret.sneer(), "Neide da Silva"); //Comment this line to get an empty name.
		return ret;
	}


	private static void setOwnName(Sneer sneer, String name) {
		sneer.profileFor(sneer.self()).setOwnName(name);
	}

	public static IFn var(String ns, String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read(ns));
		return Clojure.var(ns + "/" + simpleName);
	}

	public static SneerAdmin newSneerAdmin(PrivateKey prik, Object network, Object tupleBase) {
		return (SneerAdmin) adminVar("new-sneer-admin").invoke(prik, network, tupleBase);
	}

	private static IFn adminVar(String simpleName) {
		return var("sneer.admin", simpleName);
	}
	


	private static SneerAdmin initialize(Context context) throws FriendlyException {
		
		SneerSqliteDatabase.selfTest();
		
		File admin = new File(context.getFilesDir(), "admin");
		admin.mkdirs();
		File secureFile = new File(admin, "tupleSpace.sqlite");
		try {
			Object network = networkSimulator("new-network").invoke();
			Object tupleBase = SneerSqliteDatabase.openTupleBase(secureFile);
			return newSneerAdmin(Keys.createPrivateKey(), network, tupleBase);
		} catch (IOException e) {
			throw new FriendlyException("Problem preparing Sneer ("+e.getMessage()+")");
		}
	}
	
	
	private static void finishWith(String message, final Activity activity) {
		if (errorDialog != null) {
			activity.finish();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						errorDialog.dismiss();
						errorDialog = null;
						activity.finish();
					}
				});
		errorDialog = builder.create();
		errorDialog.show();
	}


	public static boolean checkOnCreate(Activity activity) {
		if (error == null) return true;

		finishWith(error, activity);
		return false;
	}

}
