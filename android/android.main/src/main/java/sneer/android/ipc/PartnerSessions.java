package sneer.android.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import sneer.Conversations;
import sneer.Session;
import sneer.commons.exceptions.Exceptions;

public class PartnerSessions extends Service {

	public static final String TOKEN = "TOKEN";
	private static Conversations conversations;

	class SessionHandler extends Handler {
		private final Session session;

		public SessionHandler(Session session) {
			this.session = session;
		}

		@Override
		public void handleMessage(Message msg) {
			session.send(msg.obj);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Session session = conversations.findSessionById(intent.getExtras().getLong(TOKEN));
		if (session != null)
			return new Messenger(new SessionHandler(session)).getBinder();
		else
			return null;
	}

	public static Intent intentFor(Session session) {
		return new Intent()
				.setClassName("sneer.main", PartnerSessions.class.getName())
				.putExtra(TOKEN, session.id());
	}

	public static void init(Conversations conversations) {
		Exceptions.check(PartnerSessions.conversations == null);
		PartnerSessions.conversations = conversations;
	}
}
