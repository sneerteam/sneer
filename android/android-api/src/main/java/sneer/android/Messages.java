package sneer.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import sneer.android.impl.Utils;
import sneer.android.impl.Value;

import static sneer.android.impl.IPCProtocol.*;

public class Messages {

    public static void send(Intent intent, String label, Object payload, byte[] jpegImage) {
        ResultReceiver toSneer = Utils.getExtra(intent, RESULT_RECEIVER);
        send(toSneer, label, payload, jpegImage);
    }

	static void send(ResultReceiver toSneer, String label, Object payload, byte[] jpegImage) {
		Bundle bundle = new Bundle();
		bundle.putString(LABEL, label);
        bundle.putParcelable(PAYLOAD, Value.of(payload));
		bundle.putByteArray(JPEG_IMAGE, jpegImage);
		toSneer.send(Activity.RESULT_OK, bundle);
	}


    public static String messageLabel(Intent intent) {
        return intent == null ? null : intent.getStringExtra(LABEL);
    }


    public static Object messagePayload(Intent intent) {
        if (intent == null) return null;
        Value envelope = intent.getParcelableExtra(PAYLOAD);
        if (envelope == null) return null;
        return envelope.get();
    }


    public static byte[] messageJpegImage(Intent intent) {
        return (intent == null)
            ? null
            : intent.getByteArrayExtra(JPEG_IMAGE);
    }
}
