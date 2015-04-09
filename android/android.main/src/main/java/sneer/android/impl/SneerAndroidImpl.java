package sneer.android.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.admin.SneerAdminFactory;
import sneer.android.SneerAndroid;
import sneer.android.database.SneerSqliteDatabase;
import sneer.android.ipcold.PluginManager;
import sneer.android.utils.AndroidUtils;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;

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
		logPublicKey();
		initPlugins(context);
	}


	private void logPublicKey() {
		System.out.println("YOUR PUBLIC KEY IS: " + pukAddress());
	}


	private String pukAddress() {
		return sneerAdmin.privateKey().publicKey().toHex();
	}


	private void initPlugins(Context context) {
		pluginManager = new PluginManager(context, sneer());
		pluginManager.initPlugins();
	}


	private SneerAdmin newSneerAdmin(Context context) throws FriendlyException {
		File dbFile = secureFile(context, "tupleSpace.sqlite");
		try {
			return newSneerAdmin(SneerSqliteDatabase.openDatabase(dbFile));
		} catch (IOException e) {
			SystemReport.updateReport("Error starting Sneer", e);
			throw new FriendlyException("Error starting Sneer", e);
		}
	}


	private File secureFile(Context context, String name) {
		return new File(secureDir(context), name);
	}


	private File secureDir(Context context) {
		File adminDir = new File(context.getFilesDir(), "admin");
		adminDir.mkdirs();
		return adminDir;
	}


	private static SneerAdmin newSneerAdmin(SneerSqliteDatabase db) {
		try {
			return SneerAdminFactory.create(db);
		} catch (Throwable e) {
			log(e);
			throw new RuntimeException(e);
		}
	}


	private static void log(Throwable e) {
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		for (String line : writer.toString().split("\\n"))
			Log.d("sneer-admin-factory", line);
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
