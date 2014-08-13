package sneer.android.ui;

import sneer.*;
import android.os.*;

public class InteractionActivity extends SneerActivity {

	private Session session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		session = SneerAndroid.sessionOnAndroidMainThread(this);
	}

	
	@Override
	protected void onDestroy() {
		session.dispose();
		session = null;
		super.onDestroy();
	}

	
	public Session session() {
		return session;
	}

	
}
