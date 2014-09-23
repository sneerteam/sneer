package sneer.android.main.ipc;

import java.io.Serializable;

import sneer.PublicKey;
import sneer.Sneer;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.Tuple;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

public class PluginHandler implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String packageName;
	private String activityName;
	private PluginType pluginType;
	private int menuIcon;
	private String tupleType;
	private String menuCaption;
	private String notificationLabel;
	private Context context;
	private Sneer sneer;
	
	public PluginHandler(Context context, Sneer sneer, ActivityInfo activityInfo) throws FriendlyException {
		this.context = context;
		this.sneer = sneer;
		Bundle meta = activityInfo.metaData;
		String tupleType = PluginMonitor.getString(meta, "sneer:tuple-type");
		String menuCaption = PluginMonitor.getString(meta, "sneer:menu-caption", tupleType);
		this.packageName = activityInfo.packageName;
		this.activityName = activityInfo.name;
		this.pluginType = PluginMonitor.pluginType(PluginMonitor.getString(meta, "sneer:plugin-type"));
		this.tupleType = tupleType;
		this.menuCaption = menuCaption;
		this.menuIcon = PluginMonitor.getInt(meta, "sneer:menu-icon");
		this.notificationLabel = PluginMonitor.getString(meta, "sneer:notification-label", menuCaption);
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