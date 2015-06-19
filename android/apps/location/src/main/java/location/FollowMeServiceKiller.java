package location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FollowMeServiceKiller extends BroadcastReceiver {

    public static FollowMeService victim;

    @Override
    public void onReceive(Context context, Intent intent) {
        victim.stopForeground(true);
        victim.stopSelf();
    }

}
