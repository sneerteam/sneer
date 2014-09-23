package sneer.android.main.ipc;

import sneer.Sneer;
import sneer.android.main.SneerAndroidCore.SessionIdDispenser;
import android.content.Context;

public interface PluginSessionFactory {
	PluginSession create(Context context, Sneer sneer, PluginInfo plugin, SessionIdDispenser session);
}
