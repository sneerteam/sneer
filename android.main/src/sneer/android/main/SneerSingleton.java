package sneer.android.main;

import java.io.*;

import sneer.*;
import sneer.admin.*;
import sneer.admin.impl.*;
import sneer.commons.exceptions.*;
import sneer.impl.simulator.*;
import android.content.*;

public class SneerSingleton {
	
	private static final boolean USE_SIMULATOR = true;
	
	private static SneerAdmin ADMIN = null;
	
	
	public static Sneer sneer() {
		return admin().sneer();
	}
	
	
	public static SneerAdmin admin() {
		if (ADMIN == null) throw new IllegalStateException("You must call the initialize method before you call this method.");
		return ADMIN;
	}

	
	synchronized
	public static void initializeIfNecessary(Context context) throws FriendlyException {
		if (ADMIN != null) return;

		ADMIN = USE_SIMULATOR
			? simulator()
			: initialize(context);
	}

	
	private static SneerAdmin simulator() {
		SneerAdminSimulator ret = new SneerAdminSimulator();
		setOwnName(ret.sneer(), "Neide da Silva"); //Comment this line to get an empty name.
		return ret;
	}


	private static void setOwnName(Sneer sneer, String name) {
		sneer.profileFor(sneer.self()).setOwnName(name);
	}


	private static SneerAdmin initialize(Context context) throws FriendlyException {
		File secureFolder = new File(context.getFilesDir(), "admin");
		return new SneerFactoryImpl().open(secureFolder);
	}

}
