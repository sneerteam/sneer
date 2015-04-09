package sneer.android.ipcold;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.io.Serializable;

import sneer.PublicKey;
import sneer.Sneer;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.Tuple;

public class PluginHandler implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String packageName;
	private final String activityName;
	private final PluginType pluginType;
	private final int menuIcon;
	private final String tupleType;
	private final String menuCaption;
	private final String notificationLabel;
	private final Context context;
	private final Sneer sneer;


	public PluginHandler(Context context, Sneer sneer, ActivityInfo activityInfo) throws FriendlyException {
		this.context = context;
		this.sneer = sneer;
		Bundle meta = activityInfo.metaData;
		packageName = activityInfo.packageName;
		activityName = activityInfo.name;
		pluginType = PluginMonitor.pluginType(PluginMonitor.getString(meta, "sneer:plugin-type"));
		tupleType = PluginMonitor.getString(meta, "sneer:tuple-type");
		menuCaption = PluginMonitor.getString(meta, "sneer:menu-caption", tupleType);
		menuIcon = PluginMonitor.getInt(meta, "sneer:menu-icon");
		notificationLabel = PluginMonitor.getString(meta, "sneer:notification-label", menuCaption);
	}


	public boolean canCompose() {
		return pluginType.canCompose;
	}


	public Boolean canView() {
		return pluginType.canView;
	}


	@Override
	public String toString() {
		return activityName + "(" + tupleType + ")";
	}


	public Intent createIntent() {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(packageName, activityName);
		return intent;
	}


	public boolean isSamePackage(String packageName) {
		return this.packageName.equals(packageName);
	}


	public Drawable drawableMenuIcon(Context context) throws NotFoundException, NameNotFoundException {
		return context.getPackageManager().getResourcesForApplication(packageName).getDrawable(menuIcon);
	}


	public void start(PublicKey partner) {
		pluginType.factory.create(context, sneer, this).startNewSessionWith(partner);
	}


	public Intent resume(Tuple tuple) {
		return pluginType.factory.create(context, sneer, this).createResumeIntent(tuple);
	}


	public String tupleType() {
		return tupleType;
	}


	public String menuCaption() {
		return menuCaption;
	}


	public String notificationLabel() {
		return notificationLabel;
	}

}
