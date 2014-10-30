package fabiomanera.sendlocation;

import sneer.android.ui.MessageActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ViewLocationActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		String url = (String)messagePayload();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		finish();
	}


	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}
	
}
