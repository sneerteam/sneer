package sneer.android.ipc;

import sneer.Sneer;
import android.content.Context;

public final class SingleMessageSessionFactory implements PluginSessionFactory {
	
	public static PluginSessionFactory singleton = new SingleMessageSessionFactory();

	@Override public PluginSession create(Context context, Sneer sneer, PluginHandler plugin) {
		return new SingleMessageSession(context, sneer, plugin);
	}
	
}