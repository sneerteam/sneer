package sneer.android.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.Sneer;
import sneer.android.utils.LogUtils;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.ObservedSubject;

public class PluginMonitor extends BroadcastReceiver {

	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;
	private static ObservedSubject<List<PluginHandler>> plugins = ObservedSubject.create((List<PluginHandler>)new ArrayList<PluginHandler>());
	private static Sneer sneer;


	@Override
	public void onReceive(Context context, Intent intent) {
		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()))
			packageAdded(context, packageName);
		else
			packageRemoved(context, packageName);
	}


	static String getString(Bundle bundle, String key) throws FriendlyException {
		requiredMetadata(bundle, key);
		return getString(bundle, key, null);
	}


	static String getString(Bundle bundle, String key, String def) throws FriendlyException {
		return bundle.getString(key, def);
	}


	static int getInt(Bundle bundle, String key) throws FriendlyException {
		requiredMetadata(bundle, key);
		return bundle.getInt(key);
	}


	private static void requiredMetadata(Bundle bundle, String key) throws FriendlyException {
		if (!bundle.containsKey(key))
			throw new FriendlyException("Missing meta-data " + key);
	}


	public static Observable<ActivityInfo> filterPlugins(Observable<PackageInfo> packageInfos) {
		return packageInfos.filter(new Func1<PackageInfo, Boolean>() { @Override public Boolean call(PackageInfo info) {
			return info.activities != null;
		}})
		.flatMap(new Func1<PackageInfo, Observable<ActivityInfo>>() { @Override public Observable<ActivityInfo> call(PackageInfo packageInfo) {
			return Observable.from(packageInfo.activities);
		}})
		.filter(new Func1<ActivityInfo, Boolean>() { @Override public Boolean call(ActivityInfo info) {
			return info.metaData != null && info.metaData.getString("sneer:plugin-type") != null;
		}});
	}


	public static void initialDiscovery(Context context, Sneer sneer) {
		PluginMonitor.sneer = sneer;
		LogUtils.info(PluginMonitor.class, "Searching for Sneer plugins...");

		final PackageManager pm = context.getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		Observable<PackageInfo> packages = Observable.from(apps).filter(new Func1<ApplicationInfo, Boolean>() { @Override public Boolean call(ApplicationInfo ai) {
			return (ai.metaData != null && ai.metaData.get("SneerApp") != null);
		}}).flatMap(new Func1<ApplicationInfo, Observable<PackageInfo>>() { @Override public Observable<PackageInfo> call(ApplicationInfo applicationInfo) {
			try {
				return Observable.just(pm.getPackageInfo(applicationInfo.packageName, PACKAGE_INFO_FLAGS));
			} catch (NameNotFoundException e) {
				SystemReport.updateReport(applicationInfo.packageName, "Failed to read application information: " + e.getMessage());
				return Observable.empty();
			}
		}});

		filterPlugins(packages)
			.flatMap(fromActivity(context))
			.toList()
			.subscribe(pluginsListPublisher());

		LogUtils.info(PluginMonitor.class, "Done.");
	}


	private static Func1<ActivityInfo, Observable<PluginHandler>> fromActivity(final Context context) {
		return new Func1<ActivityInfo, Observable<PluginHandler>>() { @Override public Observable<PluginHandler> call(ActivityInfo activityInfo) {
			try {
				return Observable.just(new PluginHandler(context, sneer, activityInfo));
			} catch (FriendlyException e) {
				SystemReport.updateReport(activityInfo.packageName, "Failed to read package information: " + e.getMessage());
				return Observable.empty();
			}
		}};
	}


	public static void packageAdded(Context context, String packageName) {
		try {
			LogUtils.info(PluginMonitor.class, "Package added: " + packageName);
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PACKAGE_INFO_FLAGS);

			filterPlugins(Observable.just(packageInfo))
				.flatMap(fromActivity(context))
				.concatWith(currentKnownPlugins())
				.toList()
				.subscribe(pluginsListPublisher());

		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}


	public static void packageRemoved(Context context, final String packageName) {
		LogUtils.info(PluginMonitor.class, "Package removed: " + packageName);

		currentKnownPlugins()
			.filter(new Func1<PluginHandler, Boolean>() { @Override public Boolean call(PluginHandler handler) {
				return !handler.isSamePackage(packageName);
			}})
			.toList()
			.subscribe(pluginsListPublisher());
	}


	private static Action1<List<PluginHandler>> pluginsListPublisher() {
		return new Action1<List<PluginHandler>>() { @Override public void call(List<PluginHandler> handlers) {
			LogUtils.info(PluginMonitor.class, "Pushing new plugin list: " + handlers);
			plugins.onNext(handlers);
		}};
	}


	private static Observable<PluginHandler> currentKnownPlugins() {
		return Observable.from(plugins.observed().current());
	}


	public static Observable<List<PluginHandler>> plugins() {
		return plugins.observed().observable();
	}


	protected static PluginType pluginType(String pluginTypeString) throws FriendlyException {
		PluginType pluginType = PluginType.valueOfOrNull(pluginTypeString.replace('/', '_'));
		if (pluginType == null) {
			throw new FriendlyException("Unknown plugin type: " + pluginTypeString);
		}
		return pluginType;
	}

}
