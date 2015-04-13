package sneer.android.ipc;

public class Plugin {

	final String packageName;
	final String activityName;

	public Plugin(String packageName, String activityName) {
		this.packageName = packageName;
		this.activityName = activityName;
	}
}
