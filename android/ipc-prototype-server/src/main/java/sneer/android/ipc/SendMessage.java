package sneer.android.ipc;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class SendMessage extends IntentService {

    public SendMessage() {
        super("SendMessage");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        final String message = intent.getExtras().getString("MESSAGE");   // TODO Create constant for extra ID. Protect from ClassCastException

        // Create a handler to post messages to the main thread
        Handler mHandler = new Handler(getMainLooper());
        mHandler.post(new Runnable() { @Override public void run() {
            Toast.makeText(getApplicationContext(), "" + message, Toast.LENGTH_SHORT).show();
        }});

    }

}
