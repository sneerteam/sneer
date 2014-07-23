package sneer.android.main;

import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;
import sneer.impl.simulator.*;

public class SneerSingleton {
	
	@Deprecated
	public static Sneer SNEER = sneer();
	private static Sneer INSTANCE = null;

	/*
	dir = new File(context.getFilesDir(), "admin");
	SneerAdmin admin = SneerAdminImpl(dir);
	 */

	public static Sneer sneer() {
//		if (INSTANCE == null) throw new IllegalStateException("You must call SneerSingleton.setInstance(...) first.");
		if (INSTANCE == null) {
			SneerAdmin admin = new SneerAdminSimulator();
			admin.initialize(Keys.createPrivateKey());
			admin.setOwnName("Neide da Silva");
			INSTANCE = admin.sneer();
		}
		return INSTANCE;
	}

}
