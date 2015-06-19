package sneer.android.ipc;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.Session;
import sneer.android.utils.AndroidUtils;

import static sneer.android.impl.IPCProtocol.IS_OWN;
import static sneer.android.impl.IPCProtocol.JOIN_SESSION;
import static sneer.android.impl.IPCProtocol.SEND_MESSAGE;

public class PluginActivities {

	public static void start(final Context context, Plugin plugin, Conversation convo) {
		start(context, plugin, convo, null);
	}

	public static void open(Context context, Session session, Conversation convo) {
		Plugin plugin = Plugins.forSessionType(context, session.type());
		if (plugin == null)
			Toast.makeText(context, "Please install an app for " + session.type(), Toast.LENGTH_LONG).show();
		else
			start(context, plugin, convo, session);
	}

	private static void start(final Context context, Plugin plugin, Conversation convo, Session session) {
		final Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(plugin.packageName, plugin.activityName);
		intent.putExtra(SEND_MESSAGE, SendMessage.intentFor(convo));

		if (plugin.partnerSessionType != null && session == null)
			convo.startSession(plugin.partnerSessionType).subscribe(new Action1<Session>() { @Override public void call(Session newSession) {
				startActivity(context, intent, newSession);
			}});
		else
			startActivity(context, intent, session);
	}

	private static void startActivity(Context context, Intent intent, Session session) {
		if (session != null) {
			intent.putExtra(JOIN_SESSION, PartnerSessions.intentFor(session));
			intent.putExtra(IS_OWN, session.isOwn());
		}
		startActivity(context, intent);
	}

	private static void startActivity(Context context, Intent intent) {
		try {
			context.startActivity(intent);
		} catch(Exception e) {
			AndroidUtils.toast(context, e.getMessage(), Toast.LENGTH_LONG);
		}
	}

}
