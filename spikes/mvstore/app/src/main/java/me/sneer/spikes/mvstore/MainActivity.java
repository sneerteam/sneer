package me.sneer.spikes.mvstore;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.io.File;


public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		MVStore s = MVStore.open(new File(this.getFilesDir(), "mvstore-file").getAbsolutePath());
		MVMap<String, Long> map = s.openMap("some-map");
		Long timeWas = map.get("time");
		Toast.makeText(this, "Time was: " + timeWas, Toast.LENGTH_LONG).show();

		map.put("time", System.currentTimeMillis());
		Long timeIs = map.get("time");
		Toast.makeText(this, "Time is: " + timeIs, Toast.LENGTH_LONG).show();

		s.commit();
		s.close();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
