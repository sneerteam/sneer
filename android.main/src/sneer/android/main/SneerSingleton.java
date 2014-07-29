package sneer.android.main;

import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;
import sneer.impl.simulator.*;

public class SneerSingleton {
	
	private volatile static SneerAdmin ADMIN = null;
	
	/*
	dir = new File(context.getFilesDir(), "admin");
	SneerAdmin admin = SneerAdminImpl(dir);
	 */

	public static Sneer sneer() {
		return ADMIN.sneer();
	}
	
	public static SneerAdmin admin() {
		if (ADMIN == null) {
			synchronized (SneerSingleton.class) {
				if (ADMIN == null) {
					ADMIN = new SneerAdminSimulator();
					ADMIN.initialize(Keys.createPrivateKey());
					ADMIN.setOwnName("Neide da Silva");
				}
			}
		}
		return ADMIN;
	}

}
