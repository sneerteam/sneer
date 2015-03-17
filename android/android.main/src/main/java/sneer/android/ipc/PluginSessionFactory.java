package sneer.android.ipc;

import android.content.Context;

import sneer.Sneer;

public interface PluginSessionFactory {

	PluginSession create(Context context, Sneer sneer, PluginHandler plugin);

}
