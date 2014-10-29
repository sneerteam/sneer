package sneer.android.main.ipc;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.ConversationMenuItem;
import sneer.Message;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.android.main.SneerAndroidCore;
import sneer.android.main.utils.LogUtils;
import sneer.tuples.Tuple;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class PluginManager {

	private static Map<String, PluginHandler> tupleViewers = new HashMap<String, PluginHandler>();
	
	private Func1<List<PluginHandler>, Observable<List<ConversationMenuItem>>> fromSneerPluginInfoList = new Func1<List<PluginHandler>, Observable<List<ConversationMenuItem>>>() { @Override public Observable<List<ConversationMenuItem>> call(List<PluginHandler> apps) {
		return Observable.from(apps)
			.filter(new Func1<PluginHandler, Boolean>() { @Override public Boolean call(PluginHandler t1) {
				return t1.canCompose();
			}})
			.map(new Func1<PluginHandler, ConversationMenuItem>() { @Override public ConversationMenuItem call(final PluginHandler app) {
				return new ConversationMenuItemImpl(app);
			}})
			.toList();
	}};

	private Context context;
	private Sneer sneer;
	
	
	private final class ConversationMenuItemImpl implements ConversationMenuItem {
		private final PluginHandler plugin;

		private ConversationMenuItemImpl(PluginHandler app) {
			this.plugin = app;
		}

		@Override
		public void call(PublicKey partyPuk) {
			plugin.start(partyPuk);
		}

		@Override
		public byte[] icon() {
			try {
				return bitmapFor(plugin.drawableMenuIcon(context));
			} catch (Exception e) {
				LogUtils.warn(SneerAndroidCore.class, "Error loading bitmap", e);
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public String caption() {
			return plugin.menuCaption();
		}
	}
	

	public PluginManager(Context context, Sneer sneer) {
		this.context = context;
		this.sneer = sneer;
	}

	
	private static byte[] bitmapFor(Drawable icon) {
		Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		return stream.toByteArray();
	}
	
	
	public void initPlugins() {
		PluginMonitor.initialDiscovery(context, sneer);
		PluginMonitor.plugins()
			.flatMap(fromSneerPluginInfoList)
			.subscribe(new Action1<List<ConversationMenuItem>>() { @Override public void call(List<ConversationMenuItem> menuItems) {
				sneer.setConversationMenuItems(menuItems);
			}});
		
		PluginMonitor.plugins()
			.flatMap(new Func1<List<PluginHandler>, Observable<Map<String, PluginHandler>>>() { @Override public Observable<Map<String, PluginHandler>> call(List<PluginHandler> t1) {
				return Observable.from(t1)
						.filter(new Func1<PluginHandler, Boolean>() { @Override public Boolean call(PluginHandler t1) {
							return t1.canView();
						}})
						.toMap(new Func1<PluginHandler, String>() { @Override public String call(PluginHandler t1) {
							return t1.tupleType();
						}});
			}})
			.subscribe(new Action1<Map<String, PluginHandler>>() { @Override public void call(Map<String, PluginHandler> t1) {
				tupleViewers = t1;
			}});			
	}
	
	
	public boolean isClickable(Message message) {
		return tupleViewers.containsKey(message.tuple().get("message-type"));
	}

	
	public void doOnClick(Message message) {
		Tuple tuple = message.tuple();
		PluginHandler viewer = tupleViewer((String)tuple.get("message-type"));
		if (viewer == null) {
			throw new RuntimeException("Can't find viewer plugin for message type '" + tuple.get("message-type") + "'");
		}
		context.startActivity(viewer.resume(tuple));
	}

	
	public PluginHandler tupleViewer(String type) {
		return tupleViewers.get(type);
	}

}
