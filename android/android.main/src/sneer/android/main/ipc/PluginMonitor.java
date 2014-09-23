package sneer.android.main.ipc;

import android.content.*;
import android.util.*;

public class PluginMonitor extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(PluginMonitor.class.getSimpleName(), "--------> " + intent.getAction() + " - " + intent.getDataString());
		
		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
			PluginInfo.packageAdded(context, packageName);
		} else {
			PluginInfo.packageRemoved(context, packageName);
		}
		
	}

}
