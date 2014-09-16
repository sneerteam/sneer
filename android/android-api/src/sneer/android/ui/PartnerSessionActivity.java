package sneer.android.ui;

import android.os.Bundle;

public abstract class PartnerSessionActivity extends SneerActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// TODO: Start replaying previous messages on separate thread.
	}


	/** Called in the Android main thread (UI thread).
	 *  @param name The current name of the peer with you in this session. */
	protected void onPartnerName(String name) {};

	
	protected void send(Object message) { /*TODO*/ }
	protected abstract void onMessageSent(Object message);
	
	protected abstract void onMessageFromPartner(Object message);

	
	/** Called in the Android main thread (UI thread) after each message, if it is the last message in this session. */
	protected abstract void update();

}
