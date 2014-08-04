package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;
import rx.*;
import rx.functions.*;
import sneer.android.main.*;
import sneer.commons.exceptions.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class MainActivity extends Activity {

	private AlertDialog error;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (!initializeSneerSingleton()) return;
		
		startActivity();
	}


	private void startActivity() {
		ownName().subscribe(new Action1<String>() { @Override public void call(String name) {
			Class<?> activity = name.isEmpty()
				? ProfileActivity.class
				: InteractionListActivity.class;
			startActivity(new Intent(MainActivity.this, activity));
		}});
	}


	private boolean initializeSneerSingleton() {
		try {
			SneerSingleton.initializeIfNecessary(getApplicationContext());
			return true;
		} catch (FriendlyException e) {
			finishWith(e.getMessage());
			return false;
		}
	}


	private void finishWith(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {
				error.dismiss();
				finish();
			}});
		error = builder.create();
		error.show();
	}

	
	private Observable<String> ownName() {
		return sneer().profileFor(sneer().self()).name();
	}

}
