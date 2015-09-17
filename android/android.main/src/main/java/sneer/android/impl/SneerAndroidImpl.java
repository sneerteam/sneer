package sneer.android.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.Contact;
import sneer.ConversationItem;
import sneer.PublicKey;
import sneer.Session;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.admin.SneerAdminFactory;
import sneer.android.SneerAndroid;
import sneer.android.database.SneerSqliteDatabase;
import sneer.android.ipc.Plugin;
import sneer.android.ipc.PluginActivities;
import sneer.android.ipc.Plugins;
import sneer.android.utils.AndroidUtils;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.convos.Convo;
import sneer.crypto.impl.KeysImpl;

public class SneerAndroidImpl implements SneerAndroid {

	private final Context context;
	private SneerAdmin sneerAdmin;
	private static String error;
	public static AlertDialog errorDialog;

	public SneerAndroidImpl(Context context) {
		this.context = context;

		try {
			init();
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
	}


	private void init() throws FriendlyException {
		sneerAdmin = newSneerAdmin(context);
		if (sneerAdmin == null) return;

		Observable.timer(60, TimeUnit.SECONDS).subscribe(
				new Action1<Long>() {
					@Override
					public void call(Long aLong) {
						//createBogusContacts();
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						throwable.printStackTrace();
					}
				});
	}


	private void createBogusContacts() {
		final Sneer sneer = sneerAdmin.sneer();
		if (sneer.contacts().toBlocking().first().size() > 0)
			return;
		final KeysImpl keys = new KeysImpl();
		every(1, TimeUnit.SECONDS)
				.take(42)
				.subscribe(
						new Action1<Long>() {
							@Override
							public void call(Long aLong) {
								String contactName = "Contact " + aLong;
								try {
									PublicKey contactPuk = keys.createPublicKey(contactName.getBytes());
									final Contact c = sneer.produceContact(contactName, sneer.produceParty(contactPuk), null);
									every(5, TimeUnit.SECONDS).take(5).subscribe(
											new Action1<Long>() {
												@Override
												public void call(Long aLong) {
//												sneer.convos().withContact(c).sendMessage("Message ");
												}
											});

								} catch (FriendlyException e) {
									e.printStackTrace();
								}
							}
						});
	}

	private Observable<Long> every(int t, TimeUnit unit) {
		return Observable.timer(t, t, unit, AndroidSchedulers.mainThread());
	}


	private SneerAdmin newSneerAdmin(Context context) throws FriendlyException {
		String dbFileName = "tupleSpace.sqlite";
		File dbFile = secureFile(context, dbFileName);
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
	public List<Plugin> plugins() {
		return Plugins.all(context);
	}


	@Override
	public Sneer sneer() {
		return admin().sneer();
	}


	@Override
	public boolean isClickable(ConversationItem item) {
		return item instanceof Session;
	}

	@Override
	public void doOnClick(ConversationItem item, Convo convo) {
		if (item instanceof Session)
			//PluginActivities.open(context, (Session)item, convo);
			System.out.println("CHANGE TO NEW SESSION COMPONENTS.");
		else
			Toast.makeText(context, "Message clicked: " + item, Toast.LENGTH_LONG).show();
	}

}
