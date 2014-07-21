package sneer.android.main;

import android.app.*;
import sneer.admin.*;
import sneer.impl.simulator.*;

public class SneerSingleton extends Application {
	
	public static final SneerAdmin SNEER_ADMIN =
		new SneerAdminSimulator();
//		new SneerAdminImpl();
}
