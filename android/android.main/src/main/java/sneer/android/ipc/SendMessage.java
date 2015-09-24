package sneer.android.ipc;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import sneer.convos.Convo;

import static sneer.android.SneerAndroidFlux.dispatch;

public class SendMessage extends IntentService {

	public static final String TOKEN = "TOKEN";

	public SendMessage() {
        super("SendMessage");
    }


	static Intent intentFor(long convoId) {
		return new Intent()
			.setClassName("sneer.main", SendMessage.class.getName())
			.putExtra(TOKEN, convoId);
	}


    @Override
    protected void onHandleIntent(Intent intent) {
		if (intent == null) return;
		try {
			tryToHandle(intent);
		} catch (RuntimeException x) {
			Log.d(getClass().getName(), "Unable to send message", x);
        }
    }


	private void tryToHandle(Intent intent) {
		Log.d(getClass().getName(), "Intent received");
		long convoId = intent.getLongExtra(TOKEN, -1);
		String message = intent.getAction();

		dispatch(Convo.sendMessage(convoId, message));
	}

}
