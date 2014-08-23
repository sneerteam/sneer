package sneer.android.main;

import java.io.*;
import java.util.*;

import rx.Observable;
import rx.functions.*;
import sneer.*;
import sneer.tuples.*;
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

	public SneerAppInfo(String packageName, String activityName, SneerAppInfo.HandlerType handler, String type, String label, int icon) {
		this.packageName = packageName;
		this.activityName = activityName;
		this.handler = handler;
		this.type = type;
		this.label = label;
		this.icon = icon;
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
	
	public static void checkPackages(Context context) {
		Log.i(SneerAppInfo.class.getSimpleName(), "Searching for Sneer apps...");
		
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(PACKAGE_INFO_FLAGS);
		
		SneerAndroid sneer = new SneerAndroid(context);
		
		filterSneerApps(Observable.from(packages))
			.map(FROM_ACTIVITY)
			.toList()
			.subscribe(sneer
					.tupleSpace()
					.publisher()
					.type("sneer/apps"));
		
		// TODO: dispose sneer
		
		Log.i(SneerAppInfo.class.getSimpleName(), "Done.");
	}

	@SuppressWarnings("unchecked")
	public static void packageAdded(Context context, String packageName) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PACKAGE_INFO_FLAGS);
			
			SneerAndroid sneer = new SneerAndroid(context);

			filterSneerApps(Observable.just(packageInfo))
				.map(FROM_ACTIVITY)
				.concatWith(Observable.from((List<SneerAppInfo>)sneer.tupleSpace().filter().type("sneer/apps").tuples().map(Tuple.TO_PAYLOAD)))
				.distinct(new Func1<SneerAppInfo, String>() {  @Override public String call(SneerAppInfo t1) {
					return t1.packageName + ":"+t1.activityName;
				}})
				.toList()
				.subscribe(sneer
					.tupleSpace()
					.publisher()
					.type("sneer/apps"));

			// TODO: dispose sneer

		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}

	@SuppressWarnings("rawtypes")
	public static void packageRemoved(Context context, final String packageName) {
			
		SneerAndroid sneer = new SneerAndroid(context);
		sneer.tupleSpace().filter().type("sneer/apps").tuples()
			.map(Tuple.TO_PAYLOAD)
			.cast(List.class)
			.flatMap(new Func1<List, Observable<SneerAppInfo>>() {  @SuppressWarnings("unchecked") @Override public Observable<SneerAppInfo> call(List t1) {
				return Observable.from((List<SneerAppInfo>)t1);
			} })
			.filter(new Func1<SneerAppInfo, Boolean>() {  @Override public Boolean call(SneerAppInfo t1) {
				return !t1.packageName.equals(packageName);
			} })
			.toList()
			.subscribe(sneer
				.tupleSpace()
				.publisher()
				.type("sneer/apps"));

		// TODO: dispose sneer
	}
}