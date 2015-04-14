package sneer.android.ipc;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class SendMessage extends IntentService {

	public static final String SEND_MESSAGE       = "SEND_MESSAGE";
	public static final String CONVERSATION_TOKEN = "CONVERSATION_TOKEN";

	public SendMessage() {
        super("SendMessage");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        final String message = intent.getStringExtra(CONVERSATION_TOKEN) + " " + intent.getAction();

        new Handler(getMainLooper()).post(new Runnable() { @Override public void run() {
            Toast.makeText(getApplicationContext(), "" + message, Toast.LENGTH_SHORT).show();
        }});

    }

}
