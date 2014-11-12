package sneer.android.ipc;

import sneer.Sneer;
import android.content.Context;

public interface PluginSessionFactory {

	PluginSession create(Context context, Sneer sneer, PluginHandler plugin);

}
