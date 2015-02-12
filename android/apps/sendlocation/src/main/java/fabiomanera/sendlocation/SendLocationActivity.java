package fabiomanera.sendlocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.widget.Toast;
import sneer.android.ui.MessageActivity;

import java.io.ByteArrayOutputStream;

import static android.location.LocationManager.GPS_PROVIDER;

public class SendLocationActivity extends MessageActivity implements LocationListener {

	private static final int TEN_SECONDS = 10000;
	private volatile LocationManager locationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(GPS_PROVIDER)) {
			toast("No GPS available");
			finish();
			return;
		}

		locationManager.requestLocationUpdates(GPS_PROVIDER, TEN_SECONDS, 0, this);
	}


	@Override
	public void onLocationChanged(Location location) {
		if (location == null) return;
		String url = "https://google.com/maps/place/" + location.getLatitude() + "," + location.getLongitude();

		Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.map);
		byte[] imageBytes = scaledDownTo(bitmap, 20 * 1024);

		send(url, null, imageBytes);
		finish();
	}


	@Override
	protected void onPause() {
		super.onPause();
		if (locationManager == null) return;
		locationManager.removeUpdates(this);
		locationManager = null;
	}


	@Override public void onProviderDisabled(String arg0) {}
	@Override public void onProviderEnabled(String arg0) {}
	@Override public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}


    public static byte[] scaledDownTo(Bitmap original, int maximumLength) {
        int side = Math.min(original.getHeight(), original.getWidth());
        Bitmap reduced = original;
        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reduced.compress(Bitmap.CompressFormat.JPEG, 100, out);
            final byte[] bytes = out.toByteArray();
            if (bytes.length <= maximumLength)
                return bytes;
            side = (int) (side * 0.9f);
            reduced = ThumbnailUtils.extractThumbnail(original, side, side);
        }
    }


    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}
