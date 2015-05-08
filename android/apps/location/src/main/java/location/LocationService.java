package location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import sneer.location.R;

import static android.location.LocationManager.GPS_PROVIDER;

public class LocationService extends Service implements LocationListener {

    public final static int SERVICE_ID = 1234;

    private static final int TEN_SECONDS = 10000;

    private volatile LocationManager locationManager;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Location")
                .setContentText("Sending your GPS location...")
                .setOnlyAlertOnce(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(GPS_PROVIDER)) {
            Toast.makeText(this, "No GPS available", Toast.LENGTH_LONG).show();
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        locationManager.requestLocationUpdates(GPS_PROVIDER, TEN_SECONDS, 0, this);
        startForeground(SERVICE_ID, builder.build());
        startKillAlarm();
        return Service.START_STICKY;
    }

    private void startKillAlarm() {
        LocationServiceKiller.victim = this;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, LocationServiceKiller.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60 * 1000 * 60, alarmIntent);
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", location.toString());
    }

    @Override public void onProviderDisabled(String arg0) { }
    @Override public void onProviderEnabled(String arg0) { }
    @Override public void onStatusChanged(String arg0, int arg1, Bundle arg2) { }

}
