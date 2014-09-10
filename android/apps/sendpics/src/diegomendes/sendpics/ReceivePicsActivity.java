package diegomendes.sendpics;

import sneer.android.ui.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class ReceivePicsActivity extends SneerActivity {

	ImageView image;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive_pics);

		image = (ImageView) findViewById(R.id.picture_received);
		image.setImageBitmap((Bitmap) getIntent().getExtras().get("image"));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.receive_pics, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
