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

public class SneerAppInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;

	enum InteractionType {
		SESSION,
		MESSAGE
	}
	
	String packageName;
	String activityName;

	SneerAppInfo.InteractionType interactionType;
	String tupleType;
	String menuCaption;
	int menuIcon;

	protected static ObservedSubject<List<SneerAppInfo>> apps = ObservedSubject.create((List<SneerAppInfo>)new ArrayList<SneerAppInfo>());

	public SneerAppInfo(String packageName, String activityName, SneerAppInfo.InteractionType interactionType, String tupleType, String menuCaption, int menuIcon) {
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
	
	public static Func1<ActivityInfo, Observable<SneerAppInfo>> FROM_ACTIVITY = new Func1<ActivityInfo, Observable<SneerAppInfo>>() {  @Override public Observable<SneerAppInfo> call(ActivityInfo activityInfo) {
		Bundle meta = activityInfo.metaData;
		try {
			return Observable.just(
				new SneerAppInfo(
					activityInfo.packageName,
					activityInfo.name,
					InteractionType.valueOf(getString(meta, "sneer:interaction-type")),
					getString(meta, "sneer:tuple-type"),
					getString(meta, "sneer:menu-caption"),
					getInt(meta, "sneer:menu-icon")));
		} catch (FriendlyException e) {
			SystemReport.updateReport(activityInfo.packageName, "Failed to read package information: " + e.getMessage());
			return Observable.empty();
		}
	}

	private String getString(Bundle bundle, String key) throws FriendlyException {
		requiredMetadata(bundle, key);
		return bundle.getString(key);
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
			.subscribe(appsListPublisher());
		
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
				.subscribe(appsListPublisher());

		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}

	private static void log(String message) {
		Log.i(SneerAppInfo.class.getSimpleName(), message);
	}

	public static void packageRemoved(Context context, final String packageName) {
			
		log("Package removed: " + packageName);
		
		currentKnownApps()
			.filter(new Func1<SneerAppInfo, Boolean>() {  @Override public Boolean call(SneerAppInfo t1) {
				return !t1.packageName.equals(packageName);
			} })
			.toList()
			.subscribe(appsListPublisher());
	}


	private static Action1<List<SneerAppInfo>> appsListPublisher() {
		return new Action1<List<SneerAppInfo>>() {  @Override public void call(List<SneerAppInfo> t1) {
			log("Pushing new app list: " + t1);
			apps.subject().onNext(t1);
		}};
	}

	private static Observable<SneerAppInfo> currentKnownApps() {
		return Observable.from(apps.observed().current());
	}

	public static Observable<List<SneerAppInfo>> apps() {
		return apps.observed().observable();
	}
}