package location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LocationServiceKiller extends BroadcastReceiver {

    public static LocationService victim;

    @Override
    public void onReceive(Context context, Intent intent) {
        victim.stopForeground(true);
        victim.stopSelf();
    }

}
