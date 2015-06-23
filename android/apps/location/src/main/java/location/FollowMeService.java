package location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.HashMap;

import sneer.location.R;

import static location.LocationUtils.initProviders;

public class FollowMeService extends Service implements LocationListener {

    public final static int SERVICE_ID = 1234;
    private static final int THIRD_SECONDS = 30000;

    private volatile LocationManager locationManager;

	public static boolean isRunning;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Location")
                .setContentText("Sending your GPS location...")
                .setOnlyAlertOnce(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "No GPS available", Toast.LENGTH_LONG).show();
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        initProviders(locationManager, THIRD_SECONDS, this);
        startForeground(SERVICE_ID, builder.build());
        startKillAlarm();
		isRunning = true;
        return Service.START_STICKY;
    }

    private void startKillAlarm() {
        FollowMeServiceKiller.victim = this;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, FollowMeServiceKiller.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60 * 1000 * 60, alarmIntent);
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
		HashMap<String, Double> m = new HashMap<>();
		m.put("latitude", location.getLatitude());
		m.put("longitude", location.getLongitude());
		FollowMeActivity.session.send(m);
    }

    @Override public void onProviderDisabled(String arg0) { }
    @Override public void onProviderEnabled(String arg0) { }
    @Override public void onStatusChanged(String arg0, int arg1, Bundle arg2) { }

}
