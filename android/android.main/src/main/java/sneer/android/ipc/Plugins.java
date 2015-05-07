package sneer.android.ipc;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_META_DATA;

public class Plugins {

	public static List<Plugin> all(Context context) {
	    List<Plugin> ret = new ArrayList<>();

	    for (ApplicationInfo app : installedApps(context))
	        accumulateIfPlugin(ret, context, app);

	    return ret;
	}


	static Plugin forSessionType(Context context, String type) {
		for (Plugin p : all(context))
			if (p.partnerSessionType.equals(type))
				return p;
		return null;
	}


	private static List<ApplicationInfo> installedApps(Context context) {
		return context.getPackageManager().getInstalledApplications(GET_META_DATA);
	}


	private static void accumulateIfPlugin(List<Plugin> plugins, Context context, ApplicationInfo app) {
        Plugin plugin = toPlugin(context, app);
        if (plugin != null) {
            plugins.add(plugin);
            Log.i(Plugins.class.getSimpleName(), "Sneer Plugin accumulated: " + plugin.packageName + ", Caption: " + plugin.caption);
        }
    }


	private static Plugin toPlugin(Context context, ApplicationInfo app) {
		if (!hasSneerMetaData(app)) return null;

		String packageName = app.packageName;
		ActivityInfo[] activities = getActivityInfos(context, packageName);
		if (activities == null) return null;

		String activityClassName = activities[0].name;
        CharSequence caption     = activities[0].loadLabel(context.getPackageManager());
        Drawable icon            = activities[0].loadIcon(context.getPackageManager());
		String sessionType       = sessionType(activities[0]);

		Log.d("Plugins", "" + sessionType);

        return new Plugin(caption, icon, packageName, activityClassName, sessionType);
	}


	private static String sessionType(ActivityInfo activity) {
		Bundle metaData = activity.metaData;
		return metaData == null ? null : metaData.getString("sneer:session-type");
	}


	private static ActivityInfo[] getActivityInfos(Context context, String packageName) {
		ActivityInfo[] ret;
		try {
			ret = context.getPackageManager().getPackageInfo(packageName, GET_ACTIVITIES | GET_META_DATA).activities;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(Plugins.class.getSimpleName(), "Error", e);
			return null;
		}
		if (ret.length != 1) {
			toastTooManyActivities(context, packageName, ret.length);
			return null;
		}
		return ret;
	}


	private static boolean hasSneerMetaData(ApplicationInfo app) {
		return (app.metaData != null && app.metaData.get("SneerApp") != null);
	}


    private static void toastTooManyActivities(Context context, String packageName, int numActivities) {
        Toast.makeText(context, packageName + " has " + numActivities + " activities. Should have 1 exported activity", Toast.LENGTH_LONG).show();
    }

}
