package sneer.android.ui;

import rx.*;
import sneer.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;
import android.os.*;

public class SessionActivity extends SneerActivity {

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


	protected Observed<String> peerName() {
		throw new NotImplementedYet();
	}


	protected void sendMessage(Object content) {
		throw new NotImplementedYet();
	}


	protected Observable<Object> receivedMessages() {
		throw new NotImplementedYet();
	}

}
