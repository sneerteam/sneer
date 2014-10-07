package fabiomanera.sendlocation;

import sneer.android.ui.MessageActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class SendLocationActivity extends MessageActivity implements LocationListener {

	protected LocationManager locationManager;
	private String longitude;
	private String latitude;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
//		send("Location: ", "Latitude - " + latitude + "Longitude - " + longitude);
//		finish();
	}

	@Override
	public void onLocationChanged(Location location) {
//		txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
		latitude = "" + location.getLatitude();
		longitude = "" + location.getLongitude();
	}

	@Override
	public void onProviderDisabled(String arg0) {
		Log.d("Latitude","disable");
	}

	@Override
	public void onProviderEnabled(String arg0) {
		Log.d("Latitude","enable");
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		Log.d("Latitude","----------> status");		
	}

}
