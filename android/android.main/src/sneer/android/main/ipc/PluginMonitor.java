package sneer.android.main.ipc;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.Sneer;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.ObservedSubject;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

public class PluginMonitor extends BroadcastReceiver {
	
	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;
	private static ObservedSubject<List<PluginHandler>> plugins = ObservedSubject.create((List<PluginHandler>)new ArrayList<PluginHandler>());
	private static Sneer sneer;
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
			packageAdded(context, packageName);
		} else {
			packageRemoved(context, packageName);
		}
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
		return packageInfos.filter(new Func1<PackageInfo, Boolean>() {  @Override public Boolean call(PackageInfo t1) {
			return t1.activities != null;
		}})
		.flatMap(new Func1<PackageInfo, Observable<ActivityInfo>>() {  @Override public Observable<ActivityInfo> call(PackageInfo packageInfo) {
			return Observable.from(packageInfo.activities);
		}})
		.filter(new Func1<ActivityInfo, Boolean>() {  @Override public Boolean call(ActivityInfo t1) {
			return t1.metaData != null && t1.metaData.getString("sneer:plugin-type") != null;
		}});
	}
	
	
	public static void initialDiscovery(Context context, Sneer sneer) {
		PluginMonitor.sneer = sneer;
		log("Searching for Sneer plugins...");
		
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(PACKAGE_INFO_FLAGS);
		
		filterPlugins(Observable.from(packages))
			.flatMap(fromActivity(context))
			.toList()
			.subscribe(pluginsListPublisher());
		
		log("Done.");
	}

	
	private static Func1<ActivityInfo, Observable<PluginHandler>> fromActivity(final Context context) {
		return new Func1<ActivityInfo, Observable<PluginHandler>>() {  @Override public Observable<PluginHandler> call(ActivityInfo activityInfo) {
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
			log("Package added: " + packageName);
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

	
	private static void log(String message) {
		Log.i(PluginMonitor.class.getSimpleName(), message);
	}

	
	public static void packageRemoved(Context context, final String packageName) {
			
		log("Package removed: " + packageName);
		
		currentKnownPlugins()
			.filter(new Func1<PluginHandler, Boolean>() {  @Override public Boolean call(PluginHandler t1) {
				return !t1.isSamePackage(packageName);
			}})
			.toList()
			.subscribe(pluginsListPublisher());
	}


	private static Action1<List<PluginHandler>> pluginsListPublisher() {
		return new Action1<List<PluginHandler>>() {  @Override public void call(List<PluginHandler> t1) {
			log("Pushing new plugin list: " + t1);
			plugins.onNext(t1);
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
