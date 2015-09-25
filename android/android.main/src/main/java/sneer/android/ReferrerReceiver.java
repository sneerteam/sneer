package sneer.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import sneer.android.ui.AcceptInviteActivity;

import static sneer.android.ui.AcceptInviteActivity.REFERRER_CODE;

public class ReferrerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        String referrerString = extras.getString("referrer");

        Log.d(getClass().getName(), "Referrer is: " + referrerString);
        Toast.makeText(context, "REFERRER: " + referrerString, Toast.LENGTH_LONG).show();

        Intent accept = new Intent(context, AcceptInviteActivity.class);
        accept.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        accept.putExtra(REFERRER_CODE, referrerString);
        context.startActivity(accept);
    }
}
