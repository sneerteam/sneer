package sneer.android.main;

import static sneer.android.main.ipc.TupleSpaceService.startTupleSpaceService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.ConversationMenuItem;
import sneer.Message;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.main.core.SneerSqliteDatabase;
import sneer.android.main.ipc.PluginHandler;
import sneer.android.main.ipc.PluginMonitor;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.Tuple;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

public class SneerAndroidCore implements SneerAndroid {
	
	Func1<List<PluginHandler>, Observable<List<ConversationMenuItem>>> fromSneerPluginInfoList = new Func1<List<PluginHandler>, Observable<List<ConversationMenuItem>>>() {  @Override public Observable<List<ConversationMenuItem>> call(List<PluginHandler> apps) {
		return Observable.from(apps)
			.filter(new Func1<PluginHandler, Boolean>() {  @Override public Boolean call(PluginHandler t1) {
				return t1.canCompose();
			} })
			.map(new Func1<PluginHandler, ConversationMenuItem>() { @Override public ConversationMenuItem call(final PluginHandler app) {
				return new ConversationMenuItemImpl(app);
			} })
			.toList();
	} };

	private final class ConversationMenuItemImpl implements ConversationMenuItem {
		
		private final PluginHandler plugin;

		private ConversationMenuItemImpl(PluginHandler app) {
			this.plugin = app;
		}

		@Override
		public void call(PublicKey partyPuk) {
			plugin.start(context, sneer(), partyPuk);
		}

		@Override
		public byte[] icon() {
			try {
				return bitmapFor(plugin.drawableMenuIcon(context));
			} catch (Exception e) {
				Log.w(SneerAndroidCore.class.getSimpleName(), "Error loading bitmap", e);
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public String caption() {
			return plugin.menuCaption();
		}
	}

	private SneerAdmin sneerAdmin;
	private Context context;
	private static String error;
	private static AlertDialog errorDialog;
	private static Map<String, PluginHandler> tupleViewers = new HashMap<String, PluginHandler>();
	
	
	private static byte[] bitmapFor(Drawable icon) {
		Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		return stream.toByteArray();
	}

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
		this.context = context;
		
		try {
			sneerAdmin = initialize(context);
		} catch (FriendlyException e) {
			error = e.getMessage();
		}
		
		initPlugins();
		startTupleSpaceService(context);
	}

	private void initPlugins() {
		PluginMonitor.initialDiscovery(context);
		PluginMonitor.plugins()
			.flatMap(fromSneerPluginInfoList)
			.subscribe(new Action1<List<ConversationMenuItem>>() {  @Override public void call(List<ConversationMenuItem> menuItems) {
				sneer().setConversationMenuItems(menuItems);
			} });
		
		PluginMonitor.plugins()
			.flatMap(new Func1<List<PluginHandler>, Observable<Map<String, PluginHandler>>>() {  @Override public Observable<Map<String, PluginHandler>> call(List<PluginHandler> t1) {
				return Observable.from(t1)
						.filter(new Func1<PluginHandler, Boolean>() {  @Override public Boolean call(PluginHandler t1) {
							return t1.canView();
						} })
						.toMap(new Func1<PluginHandler, String>() {  @Override public String call(PluginHandler t1) {
							return t1.tupleType();
						}});
			} })
			.subscribe(new Action1<Map<String, PluginHandler>>() {  @Override public void call(Map<String, PluginHandler> t1) {
				tupleViewers = t1;
			} });
	}

	@Override
	public boolean isClickable(Message message) {
		return tupleViewers.containsKey(message.tuple().type());
	}


	@Override
	public void doOnClick(Message message) {
		Tuple tuple = message.tuple();
		PluginHandler viewer = tupleViewers.get(tuple.type());
		if (viewer == null) {
			throw new RuntimeException("Can't find viewer plugin for message type '"+tuple.type()+"'");
		}
		viewer.resume(context, sneer(), tuple);
	}

}