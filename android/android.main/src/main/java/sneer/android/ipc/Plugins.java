package sneer.android.ipc;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_META_DATA;

public class Plugins {

	public enum AppType {TEXT, SESSION, ALL};
	public static AppType appType = AppType.ALL;

	public static List<Plugin> all(Context context) {
	    List<Plugin> ret = new ArrayList<>();

	    for (ApplicationInfo app : installedApps(context))
	        accumulateIfPlugin(ret, context, app);

	    return ret;
	}


	static Plugin forSessionType(Context context, String type) {
		for (Plugin p : all(context))
			if (type.equals(p.partnerSessionType))
				return p;
		return null;
	}


	private static List<ApplicationInfo> installedApps(Context context) {
		return context.getPackageManager().getInstalledApplications(GET_META_DATA);
	}


	private static void accumulateIfPlugin(List<Plugin> plugins, Context context, ApplicationInfo app) {
		List<Plugin> pluginList = toPlugin(context, app);
        if (pluginList != null) {
			for (Plugin p : pluginList) {
				plugins.add(p);
				Log.i(Plugins.class.getSimpleName(), "Sneer Plugin accumulated: " + p.packageName + ", Caption: " + p.caption);
			}
        }
    }


	private static List<Plugin> toPlugin(Context context, ApplicationInfo app) {
		if (!hasSneerMetaData(app)) return null;

		String packageName = app.packageName;
		ActivityInfo[] activities = getActivityInfos(context, packageName);

		if (activities == null) return null;

		List<Plugin> ret = new ArrayList<>();
		for (ActivityInfo ai : activities) {
			if (ai.exported) {
				CharSequence caption = ai.loadLabel(context.getPackageManager());
				Drawable icon = ai.loadIcon(context.getPackageManager());
				String activityClassName = ai.name;
				String sessionType = sessionType(ai);

				Log.d("Plugins", "" + sessionType);

				switch (appType) {
					case ALL:
						ret.add(new Plugin(caption, icon, packageName, activityClassName, sessionType));
						break;
					case SESSION:
						if (sessionType != null)
							ret.add(new Plugin(caption, icon, packageName, activityClassName, sessionType));
						break;
					case TEXT:
						if (sessionType == null)
							ret.add(new Plugin(caption, icon, packageName, activityClassName, sessionType));
						break;
				}

			}
		}
        return ret;
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
		return ret;
	}


	private static boolean hasSneerMetaData(ApplicationInfo app) {
		return (app.metaData != null && app.metaData.get("SneerApp") != null);
	}

}
