package sneer.android.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class InstalledPlugins extends BroadcastReceiver {

    private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_META_DATA | PackageManager.GET_ACTIVITIES;

    private static Map<String, String> plugins = new HashMap<>();

    public static Map<String, String> all() {  //Specific type not defined yet.
        return plugins;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()))
			packageAdded(context, packageName);
//		else
//			packageRemoved(context, packageName);
	}

    private void packageAdded(Context context, String packageName) {
        Log.i(getClass().getSimpleName(), "Package installed: " + packageName);

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, PACKAGE_INFO_FLAGS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(getClass().getSimpleName(), "PackageName not found: " + packageName);
        }

        ApplicationInfo appInfo = packageInfo.applicationInfo;
        if (appInfo == null || appInfo.metaData == null) return;
        if (appInfo.metaData.get("SneerApp") == null) return;

        ActivityInfo[] activities = packageInfo.activities;
        if (activities == null) return;

        if (activities.length != 1) showToastError(context, packageName, activities.length);

        plugins.put(packageName, activities[0].name);

        Log.i(getClass().getSimpleName(), "(" + plugins.size() + ") Sneer Plugin added: " + packageName);
    }

    private void showToastError(Context context, String packageName, int numActivities) {
        Toast.makeText(context, packageName + " has " + numActivities + " activities. Should have 1", Toast.LENGTH_LONG).show();
    }

}
