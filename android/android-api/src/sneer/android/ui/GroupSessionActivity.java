package sneer.android.ui;

import sneer.Message;
import android.os.Bundle;

public abstract class GroupSessionActivity extends SneerActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// TODO: Start replaying previous messages on separate thread.
	}


	/** TODO: Who is in the group? Use Party? Use RX? */

	
	protected void send(Object messageContents) { /*TODO*/ }
	protected abstract void onMessage(Message message);
	
	
	/** Called in the Android main thread (UI thread) after each message, if it is the last message in this session. */
	protected abstract void update();

}
