package sneer.android.ui;

import static sneer.android.impl.IPCProtocol.JPEG_IMAGE;
import static sneer.android.impl.IPCProtocol.PAYLOAD;

import sneer.android.Messages;
import sneer.android.impl.Value;

import android.app.Activity;
import android.content.Intent;

public abstract class MessageActivity extends Activity {

	protected void send(String label, Object payload, byte[] jpegImage) {
		Messages.send(getIntent(), label, payload, jpegImage);
	}


    protected String messageLabel()     { return Messages.messageLabel(    getIntent()); }
    protected Object messagePayload()   { return Messages.messagePayload(  getIntent()); }
    protected byte[] messageJpegImage() { return Messages.messageJpegImage(getIntent()); }

}