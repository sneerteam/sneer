package sneer.android.ipc;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import rx.functions.Action1;
import sneer.convos.Convo;
import sneer.convos.Convos;

import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.SneerAndroidFlux.dispatch;

public class SendMessage extends IntentService {

	public static final String TOKEN = "TOKEN";

	public SendMessage() {
        super("SendMessage");
    }


	static Intent intentFor(Convo convo) {
		return new Intent()
			.setClassName("sneer.main", SendMessage.class.getName())
			.putExtra(TOKEN, convo.id);
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
		long convoId  = intent.getLongExtra(TOKEN, -1);
		final String message = intent.getAction();

		Log.d("FELIPETEST", "convoId->" + convoId);

		component(Convos.class).getById(convoId).subscribe(new Action1<Convo>() {
			@Override
			public void call(Convo convo) {
				Log.d("FELIPETEST", "send-message->" + message);
				dispatch(convo.sendMessage(message));
			}
		});
	}

}
