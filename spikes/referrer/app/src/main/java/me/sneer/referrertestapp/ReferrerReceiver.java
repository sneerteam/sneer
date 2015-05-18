package me.sneer.referrertestapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class ReferrerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        String referrerString = extras.getString("referrer");

        Log.w("TEST", "Referrer is: " + referrerString);
	    Toast.makeText(context, "REFERRER: " + referrerString, Toast.LENGTH_LONG).show();

//	    context.startActivity(new Intent(context, MainActivity.class));
    }
}
