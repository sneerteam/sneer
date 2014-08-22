package sneer.android.ui;

import static sneer.SneerAndroid.*;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.Message;
import android.os.*;

public abstract class SessionActivity extends SneerActivity {

	private Session session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		session = new SneerAndroid(this).session(
			(Long)getExtra(SESSION_ID)
		);
		
		session.previousMessages().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Message>() { public void call(Message message) {
			onMessage(message);
		};});
		session.newMessages().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Message>() { public void call(Message message) {
			onMessage(message);
			afterNewMessage();
		};});
		
		session.peerName().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { public void call(String name) {
			onPeerName(name);
		};});
	}


	private void onMessage(Message message) {
		if (message.isOwn())
			messageReceived(message.content());
		else
			messageSent(message.content());
	}


	@Override
	protected void onDestroy() {
		session.dispose();
		super.onDestroy();
	}


	protected void sendMessage(Object content) {
		session.sendMessage(content);
	}


	/** @param name The current name of the peer on the other side of this session. */
	protected abstract void onPeerName(String name);

	
	/** Called when old sent messages are being replayed or when new messages are sent. */
	protected abstract void messageSent(Object content);
	
	
	/** Called when old received messages are being replayed or when new messages are received. */
	protected abstract void messageReceived(Object content);
	
	
	/** Called after messageSent() and messageReceived() for new messages, not for messages being replayed. */
	protected abstract void afterNewMessage();
	
	
	private Object getExtra(String extra) {
		return getIntent().getExtras().get(extra);
	}
}

