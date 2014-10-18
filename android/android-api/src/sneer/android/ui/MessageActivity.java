package sneer.android.ui;

import static sneer.SneerAndroidClient.PAYLOAD;
import static sneer.SneerAndroidClient.TEXT;
import sneer.SneerAndroidClient;
import sneer.utils.Value;
import android.content.Intent;
import android.os.ResultReceiver;

public abstract class MessageActivity extends SneerActivity {

	protected Object messagePayload() {
		Intent intent = getIntent();
		if (intent == null) return null;
		Value envelope = (Value)intent.getParcelableExtra(PAYLOAD);
		if (envelope == null) return null;
		return envelope.get();
	}
	
	
	protected String messageText() {
		return getIntent() == null ? null : getIntent().getStringExtra(TEXT);
	}

		
	protected void send(String label, Object payload) {
		ResultReceiver resultReceiver = getExtra(SneerAndroidClient.RESULT_RECEIVER);
		SneerAndroidClient.send(resultReceiver, label, payload);
	}

}