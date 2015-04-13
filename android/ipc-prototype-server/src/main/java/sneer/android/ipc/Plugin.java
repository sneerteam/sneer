package sneer.android.ipc;

public class Plugin {

	final String packageName;
	final String activityName;
    final CharSequence caption;

    public Plugin(CharSequence caption, String packageName, String activityName) {
		this.packageName = packageName;
		this.activityName = activityName;
        this.caption = caption;
    }

}
