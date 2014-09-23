package sneer.android.main;

import static sneer.android.main.ipc.TupleSpaceService.startTupleSpaceService;

import java.io.File;
import java.io.IOException;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.main.core.SneerSqliteDatabase;
import sneer.android.main.ipc.PluginManager;
import sneer.android.main.utils.AndroidUtils;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

public class SneerAndroidCore implements SneerAndroid {
	
	private SneerAdmin sneerAdmin;
	private static String error;
	public static AlertDialog errorDialog;
	private PluginManager pluginManager;
	
	public SneerAndroidCore(Context context) {
		try {
			init(context);
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
	}

	private void init(Context context) throws FriendlyException {
		sneerAdmin = newSneerAdmin(context);
		pluginManager = new PluginManager(context, sneer());
		pluginManager.initPlugins();
		startTupleSpaceService(context);
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

}
