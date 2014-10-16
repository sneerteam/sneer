package fabiomanera.sendlocation;

import sneer.android.ui.*;
import android.content.*;
import android.content.DialogInterface.*;
import android.net.Uri;
import android.os.*;

public class ViewLocationActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Object location = message();
		
		String uri = (String)location;
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		startActivity(intent);
		
		finish();
	}
	
}
