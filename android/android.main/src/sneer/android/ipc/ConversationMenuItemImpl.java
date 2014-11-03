package sneer.android.ipc;

import sneer.ConversationMenuItem;
import sneer.PublicKey;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.utils.LogUtils;

public class ConversationMenuItemImpl implements ConversationMenuItem {

	private final PluginManager pluginManager;
	private final PluginHandler plugin;

	ConversationMenuItemImpl(PluginManager pluginManager, PluginHandler app) {
		this.pluginManager = pluginManager;
		this.plugin = app;
	}

	
	@Override
	public void call(PublicKey partyPuk) {
		plugin.start(partyPuk);
	}

	
	@Override
	public byte[] icon() {
		try {
			return PluginManager.bitmapFor(plugin.drawableMenuIcon(this.pluginManager.context));
		} catch (Exception e) {
			LogUtils.error(SneerAndroidImpl.class, "Error loading bitmap", e);
			e.printStackTrace();
		}
		return null;
	}

	
	@Override
	public String caption() {
		return plugin.menuCaption();
	}
	
}