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
import sneer.android.ipcold.PluginHandler;
import sneer.android.ipcold.PluginType;
import sneer.android.utils.LogUtils;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.FriendlyException;
import sneer.rx.ObservedSubject;

public class InstalledPlugins extends BroadcastReceiver {

	public static Observable<List<Object>> all() {  //Specific type not defined yet.
		return null;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
//		String packageName = intent.getDataString().substring(intent.getDataString().indexOf(':')+1);
//		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()))
//			packageAdded(context, packageName);
//		else
//			packageRemoved(context, packageName);
	}

}
