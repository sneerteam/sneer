package sneer.android.ipc;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;

public class SendMessage extends IntentService {

    public SendMessage() {
        super("SendMessage");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) return;

        String message = intent.getExtras().getString("MESSAGE");   // TODO Create constant for extra ID. Protect from ClassCastException
        Toast.makeText(this.getApplicationContext(), "" + message, Toast.LENGTH_SHORT).show();

    }

}
