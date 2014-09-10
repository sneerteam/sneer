package sneer.android.ui;

import sneer.*;
import sneer.utils.*;
import android.os.*;

public abstract class MessageActivity extends SneerActivity {

	private ResultReceiver resultReceiver;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		Object message = getExtra(SneerAndroid.MESSAGE);
		resultReceiver = getExtra(SneerAndroid.RESULT_RECEIVER);
		
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
		Bundle bundle = new Bundle();
		bundle.putParcelable("value", Value.of(messages));
		resultReceiver.send(RESULT_OK, bundle);
		finish();
	}

}