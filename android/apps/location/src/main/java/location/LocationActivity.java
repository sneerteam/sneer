package location;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.ppeccin.sneer.location.R;


public class LocationActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        String msg = "LOCATION: " + System.currentTimeMillis();
        startService(getIntent().getExtras().<Intent>getParcelable("SEND_MESSAGE").setAction(msg));
    }

}
