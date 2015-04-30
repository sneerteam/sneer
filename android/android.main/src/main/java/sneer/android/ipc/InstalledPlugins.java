package sneer.android.ipc;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_META_DATA;

public class InstalledPlugins extends BroadcastReceiver {

	public static List<Plugin> all(Context context) {
	    List<Plugin> ret = new ArrayList<>();

	    for (ApplicationInfo app : installedApps(context))
	        accumulateIfPlugin(ret, context, app);

	    return ret;
	}


	private static List<ApplicationInfo> installedApps(Context context) {
		return context.getPackageManager().getInstalledApplications(GET_META_DATA);
	}


	private static void accumulateIfPlugin(List<Plugin> plugins, Context context, ApplicationInfo app) {
        Plugin plugin = toPlugin(context, app);
        if (plugin != null) {
            plugins.add(plugin);
            Log.i(InstalledPlugins.class.getSimpleName(), "Sneer Plugin accumulated: " + plugin.packageName + ", Caption: " + plugin.caption);
        }
    }


	private static Plugin toPlugin(Context context, ApplicationInfo app) {
		if (!hasSneerMetaData(app)) return null;

		String packageName = app.packageName;
		ActivityInfo[] activities;
		try {
			activities = context.getPackageManager().getPackageInfo(packageName, GET_ACTIVITIES).activities;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(InstalledPlugins.class.getSimpleName(), "Error", e);
			return null;
		}
		if (activities.length != 1) {
			showToastError(context, packageName, activities.length);
			return null;
		}

        String activityClassName   = activities[0].name;
        CharSequence pluginCaption = activities[0].loadLabel(context.getPackageManager());
        Drawable pluginIcon        = activities[0].loadIcon(context.getPackageManager());

		Bundle metaData;
		try {
			metaData = context.getPackageManager().getActivityInfo(new ComponentName(activities[0].packageName, activities[0].name), GET_META_DATA).metaData;
		} catch (Exception e) {
			metaData = null;
		}
		String partnerSessionType = metaData == null ? null : metaData.getString("sneer:session-type");

        return new Plugin(pluginCaption, pluginIcon, packageName, activityClassName, partnerSessionType);
	}


	private static boolean hasSneerMetaData(ApplicationInfo app) {
		return (app.metaData != null && app.metaData.get("SneerApp") != null);
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

        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(getClass().getSimpleName(), "PackageName not found: " + packageName);
	        return;
        }

        ApplicationInfo app = packageInfo.applicationInfo;
        Plugin plugin = toPlugin(context, app);

        Log.i(getClass().getSimpleName(), "Sneer Plugin added: " + plugin.packageName + ", Caption: " + plugin.caption);
        //Update the stored list of plugins

    }


    private static void showToastError(Context context, String packageName, int numActivities) {
        Toast.makeText(context, packageName + " has " + numActivities + " activities. Should have 1 exported activity", Toast.LENGTH_LONG).show();
    }

}
