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
	private InteractionType interactionType;
	private int menuIcon;
	private String tupleType;
	private String menuCaption;

	PluginHandler(String packageName, String activityName, InteractionType interactionType, String tupleType, String menuCaption, int menuIcon) {
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
		return activityName + "(" + tupleType + ")";
	}

	public void start(Context context, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(packageName, activityName);
		context.startActivity(intent);
	}

	public boolean isSamePackage(String packageName) {
		return this.packageName.equals(packageName);
	}

	public Drawable drawableMenuIcon(Context context) throws NotFoundException, NameNotFoundException {
		return context.getPackageManager().getResourcesForApplication(packageName).getDrawable(menuIcon);
	}

	public void start(Context context, Sneer sneer, PublicKey partner) {
		interactionType.factory.create(context, sneer, this).start(partner);
	}
	
	public void resume(Context context, Sneer sneer, Tuple tuple) {
		interactionType.factory.create(context, sneer, this).resume(tuple);
	}

	public String tupleType() {
		return tupleType;
	}

	public String menuCaption() {
		return menuCaption;
	}

}