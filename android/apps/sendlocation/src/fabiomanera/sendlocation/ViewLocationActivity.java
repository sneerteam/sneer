package fabiomanera.sendlocation;

import java.net.URL;

import sneer.android.ui.MessageActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ViewLocationActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_location);
	}
	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		URL url = (URL)messageUrl();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url.toString()));
		startActivity(intent);
	}
	
}
