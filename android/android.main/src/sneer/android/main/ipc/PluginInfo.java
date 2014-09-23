package sneer.android.main.ipc;

import java.io.Serializable;

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
	
}