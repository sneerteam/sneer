package location;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.ppeccin.sneer.location.R;


public class LocationActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        Intent intent = new Intent();
        intent.putExtra("MESSAGE", "LOCATION: " + System.currentTimeMillis());
        intent.setClassName("sneer.android.ipc", "sneer.android.ipc.IpcServer");
        ComponentName ser = startService(intent);
    }

}
