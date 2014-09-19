package sneer.android.main;

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

public class SneerPluginInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;

	enum InteractionType {
		@Deprecated
		SESSION,
		SESSION_PARTNER,
		MESSAGE,
		MESSAGE_VIEW(false, true),
		MESSAGE_COMPOSE(true, false);
		
		public final boolean canCompose;
		public final boolean canView;

		InteractionType() {
			this(true, true);
		}
		
		InteractionType(boolean canCompose, boolean canView) {
			this.canCompose = canCompose;
			this.canView = canView;
			
		}
	}
	
	String packageName;
	String activityName;

	SneerPluginInfo.InteractionType interactionType;
	String tupleType;
	String menuCaption;
	int menuIcon;

	protected static ObservedSubject<List<SneerPluginInfo>> plugins = ObservedSubject.create((List<SneerPluginInfo>)new ArrayList<SneerPluginInfo>());

	public SneerPluginInfo(String packageName, String activityName, SneerPluginInfo.InteractionType interactionType, String tupleType, String menuCaption, int menuIcon) {
		this.packageName = packageName;
		this.activityName = activityName;
		this.interactionType = interactionType;
		this.tupleType = tupleType;
		this.menuCaption = menuCaption;
		this.menuIcon = menuIcon;
	}

	@Override
	public String toString() {
		return "SneerAppInfo [" + menuCaption + ", " + tupleType + "]";
	}
	
	public static Func1<ActivityInfo, Observable<SneerPluginInfo>> FROM_ACTIVITY = new Func1<ActivityInfo, Observable<SneerPluginInfo>>() {  @Override public Observable<SneerPluginInfo> call(ActivityInfo activityInfo) {
		Bundle meta = activityInfo.metaData;
		try {
			return Observable.just(
				new SneerPluginInfo(
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
	}

	private String getString(Bundle bundle, String key) throws FriendlyException {
		requiredMetadata(bundle, key);
		return getString(bundle, key, null);
	}
	
	private String getString(Bundle bundle, String key, String def) throws FriendlyException {
		return bundle.getString(key, def);
	}
	
	private int getInt(Bundle bundle, String key) throws FriendlyException {
		requiredMetadata(bundle, key);
		return bundle.getInt(key);
	}

	private void requiredMetadata(Bundle bundle, String key) throws FriendlyException {
		if (!bundle.containsKey(key))
			throw new FriendlyException("Missing meta-data " + key);
	}};
	
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
		Log.i(SneerPluginInfo.class.getSimpleName(), message);
	}

	public static void packageRemoved(Context context, final String packageName) {
			
		log("Package removed: " + packageName);
		
		currentKnownApps()
			.filter(new Func1<SneerPluginInfo, Boolean>() {  @Override public Boolean call(SneerPluginInfo t1) {
				return !t1.packageName.equals(packageName);
			} })
			.toList()
			.subscribe(pluginsListPublisher());
	}


	private static Action1<List<SneerPluginInfo>> pluginsListPublisher() {
		return new Action1<List<SneerPluginInfo>>() {  @Override public void call(List<SneerPluginInfo> t1) {
			log("Pushing new app list: " + t1);
			plugins.subject().onNext(t1);
		}};
	}

	private static Observable<SneerPluginInfo> currentKnownApps() {
		return Observable.from(plugins.observed().current());
	}

	public static Observable<List<SneerPluginInfo>> plugins() {
		return plugins.observed().observable();
	}

	public boolean canCompose() {
		return interactionType.canCompose;
	}
}