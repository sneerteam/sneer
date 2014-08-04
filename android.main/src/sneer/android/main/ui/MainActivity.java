package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.io.*;

import rx.functions.*;
import sneer.android.main.*;
import sneer.impl.simulator.*;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.*;
import android.os.*;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((SneerSimulator)SneerSingleton.admin().sneer()).setSelfieToAllParties(getBitmapByteArray(R.drawable.maicon));
		sneer().profileFor(sneer().self()).name().subscribe(new Action1<String>() { @Override public void call(String name) {
			Class<?> activity = name.isEmpty()
				? ProfileActivity.class
				: InteractionListActivity.class;
			startActivity(new Intent(MainActivity.this, activity));
		}});
	}

	private byte[] getBitmapByteArray(int contact) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BitmapFactory.decodeResource(getResources(), contact).compress(CompressFormat.JPEG, 90, out);
		return out.toByteArray();
	}

}
