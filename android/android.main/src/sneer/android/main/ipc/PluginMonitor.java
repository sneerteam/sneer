package sneer.android.main.ipc;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.ObservedSubject;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.*;

public class PluginMonitor extends BroadcastReceiver {
	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;
	private static ObservedSubject<List<PluginInfo>> plugins = ObservedSubject.create((List<PluginInfo>)new ArrayList<PluginInfo>());
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
			packageAdded(context, packageName);
		} else {
			packageRemoved(context, packageName);
		}
		
	}

	public static Func1<ActivityInfo, Observable<PluginInfo>> FROM_ACTIVITY = new Func1<ActivityInfo, Observable<PluginInfo>>() {  @Override public Observable<PluginInfo> call(ActivityInfo activityInfo) {
		Bundle meta = activityInfo.metaData;
		try {
			return Observable.just(
				new PluginInfo(
					activityInfo.packageName,
					activityInfo.name,
					interactionType(getString(meta, "sneer:interaction-type")),
					getString(meta, "sneer:tuple-type"),
					getString(meta, "sneer:menu-caption", null),
					getInt(meta, "sneer:menu-icon")));
		} catch (FriendlyException e) {
			SystemReport.updateReport(activityInfo.packageName, "Failed to read package information: " + e.getMessage());
			return Observable.empty();
		}
	}};
	
	private static String getString(Bundle bundle, String key) throws FriendlyException {
		requiredMetadata(bundle, key);
		return getString(bundle, key, null);
	}
	
	private static String getString(Bundle bundle, String key, String def) throws FriendlyException {
		return bundle.getString(key, def);
	}
	
	private static int getInt(Bundle bundle, String key) throws FriendlyException {
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
		} })
		.flatMap(new Func1<PackageInfo, Observable<ActivityInfo>>() {  @Override public Observable<ActivityInfo> call(PackageInfo packageInfo) {
			return Observable.from(packageInfo.activities);
		} })
		.filter(new Func1<ActivityInfo, Boolean>() {  @Override public Boolean call(ActivityInfo t1) {
			return t1.metaData != null && t1.metaData.getString("sneer:interaction-type") != null;
		} });
	}
	
	public static void initialDiscovery(Context context) {
		log("Searching for Sneer plugins...");
		
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(PACKAGE_INFO_FLAGS);
		
		filterPlugins(Observable.from(packages))
			.flatMap(FROM_ACTIVITY)
			.toList()
			.subscribe(pluginsListPublisher());
		
		log("Done.");
	}

	public static void packageAdded(Context context, String packageName) {
		try {
			log("Package added: " + packageName);
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PACKAGE_INFO_FLAGS);
			
			filterPlugins(Observable.just(packageInfo))
				.flatMap(FROM_ACTIVITY)
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
			.filter(new Func1<PluginInfo, Boolean>() {  @Override public Boolean call(PluginInfo t1) {
				return !t1.isSamePackage(packageName);
			} })
			.toList()
			.subscribe(pluginsListPublisher());
	}


	private static Action1<List<PluginInfo>> pluginsListPublisher() {
		return new Action1<List<PluginInfo>>() {  @Override public void call(List<PluginInfo> t1) {
			log("Pushing new plugin list: " + t1);
			plugins.onNext(t1);
		}};
	}

	private static Observable<PluginInfo> currentKnownPlugins() {
		return Observable.from(plugins.observed().current());
	}

	public static Observable<List<PluginInfo>> plugins() {
		return plugins.observed().observable();
	}

	protected static InteractionType interactionType(String interactionTypeString) throws FriendlyException {
		InteractionType interactionType = InteractionType.valueOfOrNull(interactionTypeString.replace('/', '_'));
		if (interactionType == null) {
			throw new FriendlyException("Unknown interaction type: " + interactionTypeString);
		}
		return interactionType;
	}
}
