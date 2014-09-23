package sneer.android.main.ipc;

import java.io.Serializable;

import sneer.PublicKey;
import sneer.Sneer;
import sneer.tuples.Tuple;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;

public class PluginHandler implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String packageName;
	private String activityName;
	private PluginType pluginType;
	private int menuIcon;
	private String tupleType;
	private String menuCaption;
	private String notificationLabel;

	PluginHandler(String packageName, String activityName, PluginType pluginType, String tupleType, String menuCaption, int menuIcon, String notificationLabel) {
		this.packageName = packageName;
		this.activityName = activityName;
		this.pluginType = pluginType;
		this.tupleType = tupleType;
		this.menuCaption = menuCaption;
		this.menuIcon = menuIcon;
		this.notificationLabel = notificationLabel;
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

	public void start(Context context, Sneer sneer, PublicKey partner) {
		pluginType.factory.create(context, sneer, this).start(partner);
	}
	
	public void resume(Context context, Sneer sneer, Tuple tuple) {
		pluginType.factory.create(context, sneer, this).resume(tuple);
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