package sneer.android.main;

import static sneer.ClojureUtils.*;
import static sneer.SneerAndroid.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import clojure.lang.*;
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
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;

public class SneerApp extends Application {
	
	private static AlertDialog errorDialog;
	
	private static final boolean USE_SIMULATOR = true;
	
	private static SneerAdmin ADMIN = null;

	private static Context context;

	private static String error;
	
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
			.flatMap(new Func1<List<SneerAppInfo>, Observable<List<ConversationMenuItem>>>() {  @Override public Observable<List<ConversationMenuItem>> call(List<SneerAppInfo> apps) {
				return Observable.from(apps)
					.map(new Func1<SneerAppInfo, ConversationMenuItem>() {  @Override public ConversationMenuItem call(final SneerAppInfo app) {
						return new ConversationMenuItem() {
							
							@Override
							public void call(PublicKey partyPuk) {
								createSession(app, partyPuk);
							}
							
							@Override
							public byte[] icon() {								
								try {
									Drawable icon = resourceForPackage(app.packageName).getDrawable(app.icon);
									return bitmapFor(icon);
								} catch (Exception e) {
									Log.w(SneerApp.class.getSimpleName(), "Error loading bitmap", e);
									e.printStackTrace();
								}

								return null;
							}
							
							@Override
							public String caption() {
								return app.label;
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

	
	private byte[] bitmapFor(Drawable icon) {
		Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		return stream.toByteArray();
	}


	private Resources resourceForPackage(String packageName) throws NameNotFoundException {
		return getPackageManager().getResourcesForApplication(packageName);
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
	
	private static Object createNetwork() {
		final boolean serverNetwork = true;
		IFn networkFactory = serverNetwork 
			? var("sneer.networking.client", "create-network")
			: var("sneer.networking.simulator", "create-network");
		return networkFactory.invoke();
	}

	private static SneerAdmin initialize(Context context) throws FriendlyException {
		
		SneerTestUtils.selfTest();
		
		File adminDir = new File(context.getFilesDir(), "admin");
		adminDir.mkdirs();
		File secureFile = new File(adminDir, "tupleSpace.sqlite");
		//secureFile.delete();
		try {
			Object network = createNetwork();
			
			SneerAdmin admin = newSneerAdmin(network, SneerSqliteDatabase.openDatabase(secureFile));
			//createBot("bot", network, admin.sneer());
			
			return admin;
		} catch (IOException e) {
			SystemReport.updateReport("Error starting Sneer", e);
			throw new FriendlyException("Error starting Sneer", e);
		}
	}


	private static void createBot(final String baseName, final Object network, final Sneer masterSneer) {
		Observable.range(1, 10)
			.delay(1, TimeUnit.SECONDS)
			.observeOn(Schedulers.newThread())
			.subscribe(new Action1<Integer>() {  @Override public void call(Integer id) {
				try {
					
					String name = baseName +":"+id;
					
					File dbFile = File.createTempFile(name, ".sqlite");
					dbFile.deleteOnExit();
					SneerAdmin botAdmin = newSneerAdmin(network, SneerSqliteDatabase.openDatabase(dbFile));
					
					masterSneer.addContact(name, masterSneer.produceParty(botAdmin.privateKey().publicKey()));
					botAdmin.sneer().addContact("master", botAdmin.sneer().produceParty(masterSneer.self().publicKey().current()));
					
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
