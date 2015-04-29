package sneer.android.ipc;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.Session;
import sneer.android.utils.AndroidUtils;

public class PluginActivities {

	private static final String SEND_MESSAGE = "SEND_MESSAGE";
	private static final String JOIN_SESSION = "JOIN_SESSION";

	public static void start(Context context, Plugin plugin, Conversation convo) {
		final Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(plugin.packageName, plugin.activityName);
		intent.putExtra(SEND_MESSAGE, SendMessage.intentFor(convo));
		if (plugin.partnerSessionType != null)
			convo.startSession(plugin.partnerSessionType).subscribe(new Action1<Session>() {
				@Override
				public void call(Session session) {
					intent.putExtra(JOIN_SESSION, PartnerSessions.intentFor(session));
				}
			});
		else {
			startActivity(context, intent);
		}
	}

	private static void startActivity(Context context, Intent intent) {
		try {
			context.startActivity(intent);
		} catch(Exception e) {
			AndroidUtils.toast(context, e.getMessage(), Toast.LENGTH_LONG);
		}
	}
}
