package sneer.android.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import rx.Observer;
import rx.functions.Action1;
import sneer.android.impl.Envelope;
import sneer.android.impl.IPCProtocol;
import sneer.commons.SystemReport;
import sneer.convos.SessionHandle;
import sneer.convos.SessionMessage;
import sneer.convos.Sessions;
import sneer.rx.Timeline;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static android.os.Message.obtain;
import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.SneerAndroidFlux.dispatch;
import static sneer.android.impl.Envelope.envelope;
import static sneer.android.impl.IPCProtocol.*;

public class PartnerSessions extends Service {

	public static final String TOKEN = "TOKEN";
	private static final String TAG = PartnerSessions.class.getSimpleName();

	class SessionHandler extends Handler {
		private final long sessionId;
		private Messenger toApp;
		private final CountDownLatch connectionPending = new CountDownLatch(1);


		public SessionHandler(long sessionId) {
            this.sessionId = sessionId;
            Timeline<SessionMessage> messages = component(Sessions.class).messages(this.sessionId);

            messages.past.subscribe(new Observer<SessionMessage>() {
                @Override
                public void onCompleted() {
                    sendDataToApp(IPCProtocol.UP_TO_DATE);
                    ////////////////////// Set message read.
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                }

                @Override
                public void onNext(SessionMessage message) {
                    sendToApp(message);
                }
            });

            messages.future.subscribe(new Action1<SessionMessage>() {
                @Override
                public void call(SessionMessage message) {
                    sendToApp(message);
                    ///////////////////// Set message read.
                }
            });
		}

        private void sendToApp(SessionMessage message) {
            Map<String, Object> map = new HashMap<>();
            map.put(IS_OWN, message.isOwn);
            map.put(PAYLOAD, message.payload);
            sendDataToApp(map);
        }

        @Override
		public void handleMessage(Message msg) {
			Object payload = getPayload(msg);
			if (isFirstMessage()) {
				toApp = (Messenger) payload;
				connectionPending.countDown();
			} else {
                System.out.println("######## DISPATCH " + payload);
                dispatch(Sessions.Actions.sendMessage(sessionId, payload));
            }
		}

		private Object getPayload(Message msg) {
			Bundle data = msg.getData();
			data.setClassLoader(getClass().getClassLoader());
            Object envelope = data.getParcelable(ENVELOPE);
            if (envelope == null) {
                SystemReport.updateReport("Null envelope received from app IPC call");
                return null;
            } else
    			return ((Envelope)envelope).content;
		}

		private boolean isFirstMessage() {
			return toApp == null;
		}

		private void sendDataToApp(Object data) {
			Message msg = obtain();

			Bundle bundle = new Bundle();
			bundle.putParcelable(ENVELOPE, envelope(data));
			msg.setData(bundle);

			await(connectionPending);

			try {
				toApp.send(msg);
			} catch (Exception e) {
				handleException(e);
			}
		}

	}

	static private void handleException(Exception e) {
		Log.d(TAG, "Exception", e);
	}

	@Override
	public IBinder onBind(Intent intent) {
		long token = intent.getExtras().getLong(TOKEN);
		return new Messenger(new SessionHandler(token)).getBinder();
	}

	public static Intent intentFor(SessionHandle session) {
		return new Intent()
				.setClassName("sneer.main", PartnerSessions.class.getName())
				.putExtra(TOKEN, session.id);
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
