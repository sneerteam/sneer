package location;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import sneer.location.R;

import static android.location.LocationManager.GPS_PROVIDER;


public class LocationActivity extends Activity implements LocationListener {

    private LocationManager locationManager;
    private Location latestLocation;
    private TextView textAccuracy;
    private Button sendButton;
	private Intent intent;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);
        textAccuracy = (TextView) findViewById(R.id.textAccuracy);
        sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setEnabled(false);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(GPS_PROVIDER))
            textAccuracy.setText("GPS is off.");
        else
            locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 0, this);
    }


    @Override
    protected void onPause() {
        locationManager.removeUpdates(this);
        finish();
        super.onPause();
    }


    public void onSendClicked(View view) {
        Intent msg = getIntent().getParcelableExtra("SEND_MESSAGE");
        if (msg != null) {
            String url = "Location:\nhttps://google.com/maps/place/" + latestLocation.getLatitude() + "," + latestLocation.getLongitude();
            startService(msg.setAction(url));
        }
		finish();
    }


    public void onCancelClicked(View view) {
        finish();
    }


    @Override
    public void onLocationChanged(Location location) {
        latestLocation = location;
        updateTextAccuracy();
    }


    private void updateTextAccuracy() {
        Log.d(">>>> LocationChanged: ", latestLocation.toString());

        textAccuracy.post(new Runnable() {
            @Override
            public void run() {
                sendButton.setEnabled(true);
                textAccuracy.setText("Accuracy " + (int) latestLocation.getAccuracy() + " meters");
            }
        });
    }


    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }

}



