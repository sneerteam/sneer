package sneer.android.main;

import java.io.*;

import sneer.*;
import sneer.admin.*;
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
		ret.populate(); //Comment this line to get an empty Sneer instance.
		return ret;
	}


	private static SneerAdmin initialize(Context context) {
		File secureFolder = new File(context.getFilesDir(), "admin");
		return new SneerFactoryImpl().open(secureFolder);
	}

}
