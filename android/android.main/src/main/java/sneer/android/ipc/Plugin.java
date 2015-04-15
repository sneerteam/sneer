package sneer.android.ipc;

import android.graphics.drawable.Drawable;

public class Plugin {

	final String packageName;
	final String activityName;
    public final CharSequence caption;
    public final Drawable icon;

    public Plugin(CharSequence caption, Drawable icon, String packageName, String activityName) {
		this.packageName = packageName;
		this.activityName = activityName;
        this.caption = caption;
        this.icon = icon;
    }

}
