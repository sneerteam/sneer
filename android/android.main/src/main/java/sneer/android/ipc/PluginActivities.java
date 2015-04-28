package sneer.android.ipc;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import sneer.Conversation;
import sneer.android.utils.AndroidUtils;

public class PluginActivities {

	private static final String SEND_MESSAGE = "SEND_MESSAGE";

	public static void start(Context context, Plugin plugin, Conversation convo) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(plugin.packageName, plugin.activityName);
		intent.putExtra(SEND_MESSAGE, SendMessage.intentFor(convo));
		try {
			context.startActivity(intent);
		} catch(Exception e) {
			AndroidUtils.toast(context, e.getMessage(), Toast.LENGTH_LONG);
		}
	}
}
