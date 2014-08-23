package sneer.android.main;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;
import android.content.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.*;
import android.util.*;

public class SneerAppInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA;

	enum HandlerType {
		SESSION
	}
	
	String packageName;
	String activityName;

	SneerAppInfo.HandlerType handler;
	String type;
	String label;
	int icon;

	protected static BehaviorSubject<List<SneerAppInfo>> apps = BehaviorSubject.create();

	public SneerAppInfo(String packageName, String activityName, SneerAppInfo.HandlerType handler, String type, String label, int icon) {
		this.packageName = packageName;
		this.activityName = activityName;
		this.handler = handler;
		this.type = type;
		this.label = label;
		this.icon = icon;
	}

	@Override
	public String toString() {
		return "SneerAppInfo [" + label + ", " + type + "]";
	}
	
	public static Func1<ActivityInfo, SneerAppInfo> FROM_ACTIVITY = new Func1<ActivityInfo, SneerAppInfo>() {  @Override public SneerAppInfo call(ActivityInfo activityInfo) {
		Bundle meta = activityInfo.metaData;
		return new SneerAppInfo(
				activityInfo.packageName, 
				activityInfo.name, 
				HandlerType.valueOf(meta.getString("sneer:handler")), 
				meta.getString("sneer:type"),
				meta.getString("sneer:label"),
				meta.getInt("sneer:icon"));
	} };
	
	public static Observable<ActivityInfo> filterSneerApps(Observable<PackageInfo> packageInfos) {
		return packageInfos.filter(new Func1<PackageInfo, Boolean>() {  @Override public Boolean call(PackageInfo t1) {
			return t1.activities != null;
		} })
		.flatMap(new Func1<PackageInfo, Observable<ActivityInfo>>() {  @Override public Observable<ActivityInfo> call(PackageInfo packageInfo) {
			return Observable.from(packageInfo.activities);
		} })
		.filter(new Func1<ActivityInfo, Boolean>() {  @Override public Boolean call(ActivityInfo t1) {
			return t1.metaData != null && t1.metaData.getString("sneer:type") != null;
		} });
	}
	
	public static void initialDiscovery(Context context) {
		log("Searching for Sneer apps...");
		
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(PACKAGE_INFO_FLAGS);
		
		filterSneerApps(Observable.from(packages))
			.map(FROM_ACTIVITY)
			.toList()
			.subscribe(appsListPublisher());
		
		log("Done.");
	}

	public static void packageAdded(Context context, String packageName) {
		try {
			log("Package added: " + packageName);
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PACKAGE_INFO_FLAGS);
			
			filterSneerApps(Observable.just(packageInfo))
				.map(FROM_ACTIVITY)
				.concatWith(currentKnownApps())
				.buffer(100, TimeUnit.MILLISECONDS)
				.first()
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
			.buffer(100, TimeUnit.MILLISECONDS)
			.first()
			.subscribe(appsListPublisher());
	}


	private static Action1<List<SneerAppInfo>> appsListPublisher() {
		return new Action1<List<SneerAppInfo>>() {  @Override public void call(List<SneerAppInfo> t1) {
			log("Pushing new app list: " + t1);
			apps.onNext(t1);
		}};
	}

	private static Observable<SneerAppInfo> currentKnownApps() {
		return apps
			.flatMap(new Func1<List<SneerAppInfo>, Observable<SneerAppInfo>>() {  @Override public Observable<SneerAppInfo> call(List<SneerAppInfo> t1) {
				log("Current sneer apps: " + t1);
				return Observable.from(t1);
			} });
	}

	public static Observable<List<SneerAppInfo>> apps() {
		return apps.asObservable();
	}
}