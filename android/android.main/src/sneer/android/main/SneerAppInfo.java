package sneer.android.main;

import java.io.*;
import java.util.*;

import sneer.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.util.*;

public class SneerAppInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	enum HandlerType {
		SESSION
	}
	
	String packageName;
	String activityName;

	SneerAppInfo.HandlerType handler;
	String type;
	String label;
	int icon;

	public SneerAppInfo(String packageName, String activityName, SneerAppInfo.HandlerType handler, String type, String label, int icon) {
		this.packageName = packageName;
		this.activityName = activityName;
		this.handler = handler;
		this.type = type;
		this.label = label;
		this.icon = icon;
	}

	public static void checkPackages(Context context) {
		Log.i(SneerApp.class.getSimpleName(), "Searching for Sneer apps...");
		
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
		List<SneerAppInfo> apps = new ArrayList<SneerAppInfo>();
		for (PackageInfo packageInfo : packages) {
			if (packageInfo.activities == null) continue;
			for (ActivityInfo activityInfo : packageInfo.activities) {
				Bundle meta = activityInfo.metaData;
				if (meta == null) continue;
				String type = meta.getString("sneer:type");
				if (type == null) continue;
				
				Log.i(SneerApp.class.getSimpleName(), "found: " + meta.getString("label") +  " - " + type);
				
				apps.add(new SneerAppInfo(
						packageInfo.packageName, 
						activityInfo.name, 
						HandlerType.valueOf(meta.getString("handler")), 
						type,
						meta.getString("label"),
						meta.getInt("icon")));
			}
		}
		
		SneerAndroid sneer = new SneerAndroid(context);
		sneer.tupleSpace().publisher()
			.type("sneer/apps")
			.pub(apps);
		
		Log.i(SneerApp.class.getSimpleName(), "Done.");
	}
}