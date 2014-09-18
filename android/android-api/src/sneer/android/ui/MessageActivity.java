package sneer.android.ui;

import static sneer.SneerAndroidClient.LABEL;
import static sneer.SneerAndroidClient.MESSAGE;
import sneer.SneerAndroidClient;
import sneer.utils.Value;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public abstract class MessageActivity extends SneerActivity {

	protected Object message() {
		Intent intent = getIntent();
		if (intent == null) return null;
		Value envelope = (Value)intent.getParcelableExtra(MESSAGE);
		if (envelope == null) return null;
		return envelope.get();
	}
	
	
	protected String messageLabel() {
		return getIntent() == null ? null : getIntent().getStringExtra(LABEL);
	}

		
	protected void send(String label, Object message) {
		ResultReceiver resultReceiver = getExtra(SneerAndroidClient.RESULT_RECEIVER);

		Bundle bundle = new Bundle();
		bundle.putString(LABEL, label);
		bundle.putParcelable(MESSAGE, Value.of(message));
		resultReceiver.send(RESULT_OK, bundle);
	}

}