package location;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import sneer.location.R;


public class SendLocationActivity extends Activity implements LocationListener {

	private static final String TAG = "LOCATIONTEST";

	private LocationManager locationManager;

	private Location latestLocation;
    private TextView textAccuracy;
    private Button sendButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);

		textAccuracy = (TextView) findViewById(R.id.textAccuracy);
        sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setEnabled(false);

		locationManager = LocationManager.getInstance(getApplicationContext());
		List<String> providers = locationManager.getAllProviders();
		boolean hasFused = false;
		for (String provider : providers) {
			if (LocationManager.FUSED_PROVIDER.equals(provider)) {
				hasFused = true;
				break;
			}
		}

		if (hasFused) {
			try {
				locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 1000, 0, this);
			} catch (IllegalArgumentException e) {
				Log.d(TAG, "failed to request a location with fused provider" + e);
			}
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "failed to request a location with network provider" + e);
		}
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
