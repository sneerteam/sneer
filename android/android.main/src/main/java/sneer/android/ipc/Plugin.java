package sneer.android.ipc;

import android.graphics.drawable.Drawable;

public class Plugin {

	final String packageName;
	final String activityName;
    public final CharSequence caption;
    public final Drawable icon;
	public final String partnerSessionType;

	public Plugin(CharSequence caption, Drawable icon, String packageName, String activityName, String partnerSessionType) {
		this.packageName = packageName;
		this.activityName = activityName;
        this.caption = caption;
        this.icon = icon;
		this.partnerSessionType = partnerSessionType;
    }

}
