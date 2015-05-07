package sneer.android.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import sneer.Conversations;
import sneer.Session;
import sneer.android.impl.Envelope;
import sneer.commons.exceptions.Exceptions;

import static android.os.Message.obtain;
import static sneer.android.impl.Envelope.envelope;
import static sneer.android.impl.IPCProtocol.ENVELOPE;

public class PartnerSessions extends Service {

	public static final String TOKEN = "TOKEN";
	private static final String TAG = PartnerSessions.class.getSimpleName();
	private static Conversations conversations;

	class SessionHandler extends Handler {
		private final Session session;
		private Messenger toApp;

		public SessionHandler(Session session) {
			this.session = session;
		}

		@Override
		public void handleMessage(Message msg) {
//			session.send(msg.obj);
			if (toApp == null) {
				Bundle data = msg.getData();
				data.setClassLoader(getClassLoader());
				toApp = ((Messenger) ((Envelope) data.getParcelable(ENVELOPE)).content);
			}
			sendToApp("Hello!!!");
		}

		private void sendToApp(Object data) {
			android.os.Message msg = obtain();
			Bundle bundle = new Bundle();
			bundle.putParcelable(ENVELOPE, envelope(data));
			msg.setData(bundle);
			try {
				doSendToApp(msg);
			} catch (Exception e) {
				handleException(e);
			}
		}

		private void doSendToApp(android.os.Message msg) throws Exception {
			toApp.send(msg);
		}
	}

	private void handleException(Exception e) {
		Log.d(TAG, "Exception", e);
	}

	@Override
	public IBinder onBind(Intent intent) {
//		long token = intent.getExtras().getLong(TOKEN);
//		Session session = conversations.findSessionById(token);
//		if (session != null)
//		return new Messenger(new SessionHandler(session)).getBinder();
//		else {
//			Toast.makeText(this, "Conversation session not found. Token: " + token, Toast.LENGTH_LONG).show();
//			return null;
//		}
		return new Messenger(new SessionHandler(null)).getBinder();

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
