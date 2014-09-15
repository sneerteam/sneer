package sneer.android.ui;

import sneer.*;
import sneer.utils.*;
import android.content.Intent;
import android.os.*;

public abstract class MessageActivity extends SneerActivity {

	protected Object message() {
		Intent intent = getIntent();
		if (intent == null) return null;
		Value envelope = (Value)intent.getParcelableExtra(SneerAndroidClient.MESSAGE);
		if (envelope == null) return null;
		return envelope.get();
	}
	
	protected void send(Object... messages) {
		ResultReceiver resultReceiver = getExtra(SneerAndroidClient.RESULT_RECEIVER);

		Bundle bundle = new Bundle();
		bundle.putParcelable("value", Value.of(messages));
		resultReceiver.send(RESULT_OK, bundle);
	}

}