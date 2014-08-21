package sneer.android.ui;

import static sneer.SneerAndroid.*;
import rx.*;
import rx.android.schedulers.*;
import sneer.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;
import android.os.*;

public class SessionActivity extends SneerActivity {

	private Session session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		session = new SneerAndroid(this).session(
			(PublicKey)getExtra(PARTY_PUK),
			(String)getExtra(TYPE)
		);
	}


	@Override
	protected void onDestroy() {
		session.dispose();
		session = null;
		super.onDestroy();
	}


	protected Observed<String> peerName() {
		//return sneer().nameFor(session.peer());
		throw new NotImplementedYet();
	}


	protected void sendMessage(Object content) {
		session.sendMessage(content);
	}


	protected Observable<Object> receivedMessages() {
		return session.receivedMessages().observeOn(AndroidSchedulers.mainThread());
	}
	
	
	private Object getExtra(String extra) {
		return getIntent().getExtras().get(extra);
	}

}
