package location;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashMap;

import sneer.android.Message;
import sneer.android.PartnerSession;
import sneer.location.R;

import static android.location.LocationManager.GPS_PROVIDER;


public class FollowMeActivity extends Activity implements LocationListener {

    public static PartnerSession session;
    private Intent service;

	private LocationManager locationManager;
	private double myLatitude;
	private double myLongitude;
	private double theirLatitude;
	private double theirLongitude;
	private ImageView map;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_follow_me);
		map = (ImageView) findViewById(R.id.map_view);

		startSession();
    }


	private void startSession() {
		session = PartnerSession.join(this, new PartnerSession.Listener() {
			@Override
			public void onUpToDate() {
				refresh();
			}

			@Override
			public void onMessage(Message message) {
				handle(message);
			}
		});
	}

	private void refresh() {
		if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			if (!locationManager.isProviderEnabled(GPS_PROVIDER))
				Toast.makeText(this, "GPS is off", Toast.LENGTH_SHORT).show();
			else
				locationManager.requestLocationUpdates(GPS_PROVIDER, 30000, 0, this);
		}

		map.post(new Runnable() { @Override public void run() {
			int width = map.getMeasuredWidth();
			int height = map.getMeasuredHeight();

			setProgressBarIndeterminate(true);
			setProgressBarIndeterminateVisibility(true);
			setProgressBarVisibility(true);

			new MapDownloader(map, width, height, FollowMeActivity.this, session).execute(
				getMapURL(width, height)
			);
		}});
	}

	private void handle(Message message) {
		HashMap<String, Double> m = (HashMap<String, Double>) message.payload();

		if (message.wasSentByMe()) {
			myLatitude = m.get("latitude");
			myLongitude = m.get("longitude");
		}
		else {
			theirLatitude = m.get("latitude");
			theirLongitude = m.get("longitude");
		}
	}

    protected String getMapURL(int width, int height) {
        if (width > height) {
            width = 640;
            height = 640 * height/width;
        }
        else {
            height = 640;
            width = 640 * width/height;
        }

        String url = "https://maps.googleapis.com/maps/api/staticmap";
        url += "?size=" + width + "x" + height + "&scale=2";
        url += "&maptype=roadmap";
        url += "&markers=size:mid%7Ccolor:red%7C" + myLatitude + "," + myLongitude;

		if (theirLatitude != 0.0)
			url += "&markers=size:mid%7Ccolor:blue%7C" + theirLatitude + "," + theirLongitude;

        return url;
    }

	@Override
	protected void onPause() {
		super.onPause();
		service = new Intent(this, FollowMeService.class);
        startService(service);
	}

	@Override
	protected void onDestroy() {
		session.close();
		super.onDestroy();
	}

	@Override
	public void onLocationChanged(Location location) {
		HashMap<String, Double> m = new HashMap<>();
		m.put("latitude", location.getLatitude());
		m.put("longitude", location.getLongitude());
		session.send(m);
	}

	@Override public void onStatusChanged(String provider, int status, Bundle extras) { }
	@Override public void onProviderEnabled(String provider) { }
	@Override public void onProviderDisabled(String provider) { }

}
