package sneer.android.ipc;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import rx.functions.Action1;
import sneer.android.utils.AndroidUtils;
import sneer.convos.SessionHandle;
import sneer.convos.Sessions;

import static sneer.android.SneerAndroidFlux.request;
import static sneer.android.impl.IPCProtocol.IS_OWN;
import static sneer.android.impl.IPCProtocol.JOIN_SESSION;
import static sneer.android.impl.IPCProtocol.SEND_MESSAGE;

public class PluginActivities {

	public static void start(final Context context, Plugin plugin, long convoId) {
		start(context, plugin, convoId, null);
	}

	public static void open(Context context, SessionHandle session, long convoId) {
		Plugin plugin = Plugins.forSessionType(context, session.type);
		if (plugin == null)
			Toast.makeText(context, "Please install an app for " + session.type, Toast.LENGTH_LONG).show();
		else
			start(context, plugin, convoId, session);
	}

	private static void start(final Context context, final Plugin plugin, long convoId, SessionHandle session) {
		final Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(plugin.packageName, plugin.activityName);
		intent.putExtra(SEND_MESSAGE, SendMessage.intentFor(convoId));

		if (plugin.partnerSessionType != null && session == null)
			request(Sessions.Actions.startSession(convoId, plugin.partnerSessionType)).subscribe(new Action1<Long>() {
				@Override
				public void call(Long sessionId) {
					startActivity(context, intent, new SessionHandle(sessionId, plugin.partnerSessionType, true));
				}
			});
		else
			startActivity(context, intent, session);
	}

	private static void startActivity(Context context, Intent intent, SessionHandle session) {
		if (session != null) {
			intent.putExtra(JOIN_SESSION, PartnerSessions.intentFor(session));
			intent.putExtra(IS_OWN, session.isOwn);
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
