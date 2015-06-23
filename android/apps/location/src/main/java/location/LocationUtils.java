package location;

import android.location.LocationListener;
import android.util.Log;

import java.util.List;

public class LocationUtils {

	private static final String TAG = "LOCATIONTEST";

	static void initProviders(LocationManager locationManager, int minTime, LocationListener listener) {
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
				locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, minTime, 0, listener);
			} catch (IllegalArgumentException e) {
				Log.d(TAG, "failed to request a location with fused provider" + e);
			}
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0, listener);
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, 0, listener);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "failed to request a location with network provider" + e);
		}
	}

}
