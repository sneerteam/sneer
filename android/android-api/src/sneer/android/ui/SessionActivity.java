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
			(Long)getExtra(SESSION_ID),
			(PrivateKey)getExtra(OWN_PRIK)
		);
		
		session.previousMessages().observeOn(AndroidSchedulers.mainThread())
			.doOnCompleted(new Action0() {  @Override public void call() {
				onMessageReplayCompleted();
			}})
			.subscribe(new Action1<Message>() { public void call(Message message) {
				replayMessage(message);
			};});
		
		session.newMessages().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Message>() { public void call(Message message) {
			newMessage(message);
		};});
		
		session.peerName().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { public void call(String name) {
			onPeerName(name);
		};});
	}


	private void newMessage(Message message) {
		if (message.isOwn())
			newMessageSent(message.content());
		else
			newMessageReceived(message.content());
	}


	private void replayMessage(Message message) {
		if (message.isOwn())
			replayMessageSent(message.content());
		else
			replayMessageReceived(message.content());
	}


	protected void onDestroy() {
		session.dispose();
		super.onDestroy();
	}


	protected void sendMessage(Object content) {
		session.sendMessage(content);
	}


	/** @param name The current name of the peer on the other side of this session. */
	protected abstract void onPeerName(String name);

	
	/** Called for each old sent message, when old messages are being replayed. */
	protected abstract void replayMessageSent(Object message);
	
	
	/** Called for each old received message, when old messages are being replayed. */
	protected abstract void replayMessageReceived(Object message);
	
	
	/** Called for each new message that was sent using the send() method. */
	protected abstract void newMessageSent(Object message);
	
	
	/** Called for each new message as it is received. */
	protected abstract void newMessageReceived(Object message);

	/** Called after the replay of old messages has finished, so this activity can know its time to update its display. */
	protected abstract void onMessageReplayCompleted();

}

