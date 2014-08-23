package sneer.android.main;

import android.content.*;
import android.util.*;

public class AppInstallationMonitor extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(AppInstallationMonitor.class.getSimpleName(), "--------> " + intent.getAction() + " - " + intent.getDataString());
		
		SneerAppInfo.checkPackages(context);
		
	}

}
