package sneer.android.main.ipc;

import java.io.*;
import java.util.*;

import rx.Observable;
import rx.functions.*;
import sneer.commons.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;
import android.content.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.*;
import android.util.*;

public class PluginInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	

	public String packageName;
	public String activityName;

	public InteractionType interactionType;
	public String tupleType;
	public String menuCaption;
	public int menuIcon;


	public PluginInfo(String packageName, String activityName, InteractionType interactionType, String tupleType, String menuCaption, int menuIcon) {
		this.packageName = packageName;
		this.activityName = activityName;
		this.interactionType = interactionType;
		this.tupleType = tupleType;
		this.menuCaption = menuCaption;
		this.menuIcon = menuIcon;
	}
	
	public boolean canCompose() {
		return interactionType.canCompose;
	}

	public Boolean canView() {
		return interactionType.canView;
	}

	@Override
	public String toString() {
		return "SneerAppInfo [" + menuCaption + ", " + tupleType + "]";
	}
	
	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;
	private static ObservedSubject<List<PluginInfo>> plugins = ObservedSubject.create((List<PluginInfo>)new ArrayList<PluginInfo>());
	
	public static Func1<ActivityInfo, Observable<PluginInfo>> FROM_ACTIVITY = new Func1<ActivityInfo, Observable<PluginInfo>>() {  @Override public Observable<PluginInfo> call(ActivityInfo activityInfo) {
		Bundle meta = activityInfo.metaData;
		try {
			return Observable.just(
				new PluginInfo(
					activityInfo.packageName,
					activityInfo.name,
					InteractionType.valueOf(getString(meta, "sneer:interaction-type").replace('/', '_')),
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
	
	public static Observable<ActivityInfo> filterSneerApps(Observable<PackageInfo> packageInfos) {
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
		log("Searching for Sneer apps...");
		
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(PACKAGE_INFO_FLAGS);
		
		filterSneerApps(Observable.from(packages))
			.flatMap(FROM_ACTIVITY)
			.toList()
			.subscribe(pluginsListPublisher());
		
		log("Done.");
	}

	public static void packageAdded(Context context, String packageName) {
		try {
			log("Package added: " + packageName);
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PACKAGE_INFO_FLAGS);
			
			filterSneerApps(Observable.just(packageInfo))
				.flatMap(FROM_ACTIVITY)
				.concatWith(currentKnownApps())
				.toList()
				.subscribe(pluginsListPublisher());

		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}

	private static void log(String message) {
		Log.i(PluginInfo.class.getSimpleName(), message);
	}

	public static void packageRemoved(Context context, final String packageName) {
			
		log("Package removed: " + packageName);
		
		currentKnownApps()
			.filter(new Func1<PluginInfo, Boolean>() {  @Override public Boolean call(PluginInfo t1) {
				return !t1.packageName.equals(packageName);
			} })
			.toList()
			.subscribe(pluginsListPublisher());
	}


	private static Action1<List<PluginInfo>> pluginsListPublisher() {
		return new Action1<List<PluginInfo>>() {  @Override public void call(List<PluginInfo> t1) {
			log("Pushing new app list: " + t1);
			plugins.onNext(t1);
		}};
	}

	private static Observable<PluginInfo> currentKnownApps() {
		return Observable.from(plugins.observed().current());
	}

	public static Observable<List<PluginInfo>> plugins() {
		return plugins.observed().observable();
	}

}