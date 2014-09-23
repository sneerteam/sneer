package sneer.android.main;

import sneer.android.main.ipc.SneerPluginInfo;
import android.content.*;
import android.util.*;

public class AppInstallationMonitor extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(AppInstallationMonitor.class.getSimpleName(), "--------> " + intent.getAction() + " - " + intent.getDataString());
		
		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
			SneerPluginInfo.packageAdded(context, packageName);
		} else {
			SneerPluginInfo.packageRemoved(context, packageName);
		}
		
	}

}
