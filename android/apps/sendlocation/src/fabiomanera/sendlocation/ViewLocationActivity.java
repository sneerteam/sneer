package fabiomanera.sendlocation;

import java.util.HashMap;

import sneer.android.ui.MessageActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ViewLocationActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locationFrom(message());
	}

	
	private void locationFrom(Object message) {
		@SuppressWarnings("unchecked")
		HashMap<String, Double> map = (HashMap<String, Double>) message;
		double latitude = map.get("latitude");
		double longitude = map.get("longitude");
		Uri geoUri = Uri.parse("geo:" + latitude + "," + longitude);
		
		openMapFor(geoUri);

		finish();
	}

	
	private void openMapFor(Uri geoUri) {
		Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
		startActivity(intent);
	}

}
