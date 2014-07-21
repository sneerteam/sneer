package sneer.android.main;

import sneer.Sneer;
import sneer.impl.keys.Keys;
import sneer.impl.simulator.SneerAdminSimulator;
import android.app.Application;

public class SneerSingleton extends Application {
	
	public static final Sneer SNEER =
		new SneerAdminSimulator().initialize(Keys.createPrivateKey());
//		new SneerAdminImpl();
}
