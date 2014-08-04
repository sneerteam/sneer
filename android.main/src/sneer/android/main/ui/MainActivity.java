package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;
import rx.*;
import rx.functions.*;
import sneer.android.main.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ownName().subscribe(new Action1<String>() { @Override public void call(String name) {
			Class<?> activity = name.isEmpty()
				? ProfileActivity.class
				: InteractionListActivity.class;
			startActivity(new Intent(MainActivity.this, activity));
		}});
	}

	
	private Observable<String> ownName() {
		return sneer().profileFor(sneer().self()).name();
	}

}
