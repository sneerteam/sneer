package sneer.android.ipc;

import android.content.Context;
import sneer.Sneer;

public final class PartnerSessionFactory implements PluginSessionFactory {

	public static PluginSessionFactory singleton = new PartnerSessionFactory();

	@Override
	public PluginSession create(Context context, Sneer sneer, PluginHandler plugin) {
		return new PartnerSession(context, sneer, plugin);
	}

}