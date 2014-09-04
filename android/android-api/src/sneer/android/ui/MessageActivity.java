package sneer.android.ui;

import sneer.*;
import sneer.commons.exceptions.*;
import android.os.*;

public abstract class MessageActivity extends SneerActivity {


	private Object message;
	private long conversationId;


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		message = getExtra(SneerAndroid.MESSAGE);
		conversationId = getExtra(SneerAndroid.CONVERSATION_ID);
		
		if (message == null)
			composeMessage();
		else
			open(message);
	}


	/** Called by Sneer after onCreate() when the user taps the menu item for this activity. */
	protected abstract void composeMessage();
	
	
	/** Called by Sneer after onCreate() when the user taps on a received or sent message of the type registered for this activity. */
	protected abstract void open(Object message);
	
	
	/** Sends message(s) and finishes this activity. Displays a toast if there was an error sending the message(s). */
	protected void send(Object... messages) {
		try {
			new SneerAndroid(this).sendMessagesIn(conversationId, messages);
		} catch (FriendlyException e) {
			toast(e);
		}
		finish();
	}

}