package sneer.android.ui;

import android.app.Activity;
import sneer.android.Messages;

public abstract class MessageActivity extends Activity {

	protected void send(String label, Object payload, byte[] jpegImage) {
		Messages.send(getIntent(), label, payload, null);
	}


    protected String messageLabel()     { return Messages.messageLabel(    getIntent()); }
    protected Object messagePayload()   { return Messages.messagePayload(  getIntent()); }
    protected byte[] messageJpegImage() { return Messages.messageJpegImage(getIntent()); }

}