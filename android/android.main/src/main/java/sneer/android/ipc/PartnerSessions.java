package sneer.android.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import rx.functions.Action1;
import sneer.Conversations;
import sneer.Session;
import sneer.android.impl.Envelope;
import sneer.android.impl.IPCProtocol;
import sneer.commons.exceptions.Exceptions;

import static android.os.Message.obtain;
import static sneer.android.impl.Envelope.envelope;
import static sneer.android.impl.IPCProtocol.ENVELOPE;
import static sneer.android.impl.IPCProtocol.IS_OWN;
import static sneer.android.impl.IPCProtocol.PAYLOAD;

public class PartnerSessions extends Service {

	public static final String TOKEN = "TOKEN";
	private static final String TAG = PartnerSessions.class.getSimpleName();
	private static Conversations conversations;

	class SessionHandler extends Handler {
		private final Session session;
		private Messenger toApp;
		private final CountDownLatch connectionPending = new CountDownLatch(1);


		public SessionHandler(final Session session) {
			this.session = session;
			session.messages().subscribe(new Action1<Session.MessageOrUpToDate>() {
				@Override
				public void call(Session.MessageOrUpToDate messageOrUpToDate) {
					if (messageOrUpToDate.isUpToDate()) {
						await(connectionPending);
						sendToApp(IPCProtocol.UP_TO_DATE);
					} else {
						Map<String, Object> map = new HashMap<>();
						map.put(IS_OWN, messageOrUpToDate.message().isOwn());
						map.put(PAYLOAD, messageOrUpToDate.message().payload());
						sendToApp(map);
					}
				}
			});
		}

		@Override
		public void handleMessage(Message msg) {
			Object payload = getPayload(msg);
			if (isFirstMessage())
				toApp = (Messenger) payload;
			else
				session.send(payload);

			if (toApp != null)
				connectionPending.countDown();
		}

		private Object getPayload(Message msg) {
			Bundle data = msg.getData();
			data.setClassLoader(getClassLoader());
			return ((Envelope)data.getParcelable(ENVELOPE)).content;
		}

		private boolean isFirstMessage() {
			return toApp == null;
		}

		private void sendToApp(Object data) {
			Message msg = obtain();

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

		private void await(CountDownLatch latch) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void handleException(Exception e) {
		Log.d(TAG, "Exception", e);
	}

	@Override
	public IBinder onBind(Intent intent) {
		long token = intent.getExtras().getLong(TOKEN);
		Session session = conversations.findSessionById(token);
		if (session != null)
			return new Messenger(new SessionHandler(session)).getBinder();
		else {
			Toast.makeText(this, "Conversation session not found. Token: " + token, Toast.LENGTH_LONG).show();
			return null;
		}
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
