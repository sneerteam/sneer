package sneer.android.main;

import sneer.admin.*;
import sneer.impl.simulator.*;

public class SneerSingleton {
	
	public static final SneerAdmin SNEER_ADMIN =
		new SneerAdminSimulator();
//		new SneerAdminImpl();

}
