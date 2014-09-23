package sneer.android.main;

import static sneer.android.main.ipc.TupleSpaceService.startTupleSpaceService;

import java.io.File;
import java.io.IOException;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.main.core.SneerSqliteDatabase;
import sneer.android.main.ipc.PluginManager;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class SneerAndroidCore implements SneerAndroid {
	
	private SneerAdmin sneerAdmin;
	private static String error;
	private static AlertDialog errorDialog;
	private PluginManager pluginManager;
	
	@Override
	public SneerAdmin admin() {
		return sneerAdmin;
	}

	@Override
	public boolean checkOnCreate(Activity activity) {
		if (error == null) return true;
		finishWith(error, activity);
		return false;
	}

	private static void finishWith(String message, final Activity activity) {
		if (errorDialog != null) {
			activity.finish();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(message).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {
			errorDialog.dismiss();
			errorDialog = null;
			activity.finish();
		}});
		errorDialog = builder.create();
		errorDialog.show();
	}

	private SneerAdmin initialize(Context context) throws FriendlyException {
	
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

	@Override
	public Sneer sneer() {
		return admin().sneer();
	}

	private static Class<?> sneerAdminFactory() {
		try {
			return Class.forName("sneer.admin.SneerAdminFactory");
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static void toast(Context context, final String message, final int length) {
		Toast.makeText(context, message, length).show();
	}

	public static void toastOnMainThread(final Context context, final String message, final int length) {
		AndroidSchedulers.mainThread().createWorker().schedule(new Action0() { @Override public void call() {
			toast(context, message, length);
		} });
	}

	public static boolean isCoreAvailable() {
		return sneerAdminFactory() != null;
	}

	public SneerAndroidCore(Context context) {
		try {
			init(context);
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
	}

	private void init(Context context) throws FriendlyException {
		sneerAdmin = initialize(context);
		pluginManager = new PluginManager(context, sneer());
		pluginManager.initPlugins();
		startTupleSpaceService(context);
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
