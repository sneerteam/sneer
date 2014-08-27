package sneer.android.main;

import static sneer.ClojureUtils.*;
import static sneer.SneerAndroid.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.functions.*;
import rx.schedulers.*;
import sneer.*;
import sneer.admin.*;
import sneer.android.main.core.*;
import sneer.commons.*;
import sneer.commons.exceptions.*;
import sneer.impl.simulator.*;
import sneer.persistent_tuple_base.*;
import sneer.tuples.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;

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

	private static SneerAdmin newSneerAdmin(Object network, Database db) {
		return (SneerAdmin) adminVar("new-sneer-admin-over-db").invoke(network, db);
	}
	
	private static Object networkSimulator() {
		return var("sneer.networking.simulator", "new-network").invoke();
	}

	private static SneerAdmin initialize(Context context) throws FriendlyException {
		
		SneerTestUtils.selfTest();
		
		File adminDir = new File(context.getFilesDir(), "admin");
		adminDir.mkdirs();
		File secureFile = new File(adminDir, "tupleSpace.sqlite");
		try {
			Object network = networkSimulator();
			
			SneerAdmin admin = newSneerAdmin(network, SneerSqliteDatabase.openDatabase(secureFile));
			createBots("bot", network, admin.sneer());
			
			return admin;
		} catch (IOException e) {
			SystemReport.updateReport("Error starting Sneer", e);
			throw new FriendlyException("Error starting Sneer", e);
		}
	}


	private static void createBots(final String name, Object network, final Sneer masterSneer) {
		Observable.just(network)
			.observeOn(Schedulers.newThread())
			.subscribe(new Action1<Object>() {  @Override public void call(Object network) {
				try {
					SneerAdmin admin = newSneerAdmin(network, SneerSqliteDatabase.openDatabase(File.createTempFile(name, ".sqlite")));
					
					masterSneer.addContact(name, masterSneer.produceParty(admin.privateKey().publicKey()));
					admin.sneer().addContact("master", admin.sneer().produceParty(masterSneer.self().publicKey().current()));
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (FriendlyException e) {
					e.printStackTrace();
				}
			} });
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
