package sneer.android.ipcold;

import android.content.Context;

import sneer.Sneer;

public interface PluginSessionFactory {

	PluginSession create(Context context, Sneer sneer, PluginHandler plugin);

}
