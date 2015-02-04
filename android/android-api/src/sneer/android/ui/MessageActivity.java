package sneer.android.ui;

import static sneer.SneerAndroidClient.JPEG_IMAGE;
import static sneer.SneerAndroidClient.PAYLOAD;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import static sneer.SneerAndroidClient.TEXT;
import sneer.SneerAndroidClient;
import sneer.android.impl.Value;
import android.content.Intent;
import android.os.ResultReceiver;

public abstract class MessageActivity extends SneerActivity {

	protected void send(String label, Object payload, byte[] jpegImage) {
		ResultReceiver resultReceiver = getExtra(RESULT_RECEIVER);
		SneerAndroidClient.send(resultReceiver, label, payload, null);
	}

	
	protected Object messagePayload() {
		Intent intent = getIntent();
		if (intent == null) return null;

		Value envelope = (Value)intent.getParcelableExtra(PAYLOAD);
		if (envelope == null) return null;

		return envelope.get();
	}


	protected byte[] messageJpegImage() {
		Intent intent = getIntent();
		if (intent == null) return null;

		byte[] image = intent.getByteArrayExtra(JPEG_IMAGE);
		if (image == null) return null;

		return image;
	}


	protected String messageText() {
		return getIntent() == null ? null : getIntent().getStringExtra(TEXT);
	}


}