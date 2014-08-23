package sneer.android.main;

import java.io.*;
import java.util.*;

import rx.Observable;
import rx.functions.*;
import sneer.*;
import sneer.admin.*;
import sneer.admin.impl.*;
import sneer.commons.exceptions.*;
import sneer.impl.simulator.*;
import sneer.tuples.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.util.*;

public class SneerApp extends Application {
	
	private static AlertDialog errorDialog;
	
	private static final boolean USE_SIMULATOR = true;
	
	private static SneerAdmin ADMIN = null;

	private static Context context;

	private static String error;
	
	private static final String PREFS_NAME = "SneerApp";

	private static final int APPS_SEARCHER_VERSION = 2;
	
	@Override
	public void onCreate() {

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		if (settings.getInt("apps_searcher_version", 0) < APPS_SEARCHER_VERSION) {
			SneerAppInfo.checkPackages(getApplicationContext());
		    settings.edit()
		    	.putInt("apps_searcher_version", APPS_SEARCHER_VERSION)
		    	.commit(); 
		}
		
		try {
			initialize();
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
		
		sneer().tupleSpace().filter()
			.type("sneer/apps")
			.tuples()
			.map(Tuple.TO_PAYLOAD)
			.cast(List.class)
			.flatMap(new Func1<List, Observable<List<ConversationMenuItem>>>() {  @Override public Observable<List<ConversationMenuItem>> call(List t1) {
				List<SneerAppInfo> apps = t1;
				return Observable.from(apps)
					.map(new Func1<SneerAppInfo, ConversationMenuItem>() {  @Override public ConversationMenuItem call(final SneerAppInfo t1) {
						return new ConversationMenuItem() {
							
							@Override
							public void call() {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public byte[] icon() {
								// TODO Auto-generated method stub
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
			.subscribe(ConversationSimulator.menu);
		
		super.onCreate();
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


	private static SneerAdmin initialize(Context context) throws FriendlyException {
		File secureFolder = new File(context.getFilesDir(), "admin");
		return new SneerFactoryImpl().open(secureFolder);
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
