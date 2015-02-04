package sneer.android.ipc;

import static sneer.android.impl.IPCProtocol.JPEG_IMAGE;
import static sneer.android.impl.IPCProtocol.PAYLOAD;
import static sneer.android.impl.IPCProtocol.RESULT_RECEIVER;
import static sneer.android.impl.IPCProtocol.LABEL;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.utils.AndroidUtils;
import sneer.android.utils.LogUtils;
import sneer.tuples.Tuple;
import sneer.android.impl.SharedResultReceiver;
import sneer.android.impl.Value;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class SingleMessageSession implements PluginSession {

	private final Context context;
	private final Sneer sneer;
	private final PluginHandler plugin;


	SingleMessageSession(Context context, Sneer sneer, PluginHandler plugin) {
		this.context = context;
		this.sneer = sneer;
		this.plugin = plugin;
	}


	@Override
	public Intent createResumeIntent(Tuple tuple) {
		Intent intent = plugin.createIntent();

		intent.putExtra(PAYLOAD, Value.of(tuple.payload()));
		intent.putExtra(LABEL, (String)tuple.get(LABEL));
		intent.putExtra(JPEG_IMAGE, (byte[])tuple.get(JPEG_IMAGE));

		return intent;
	}


	@Override
	public void startNewSessionWith(final PublicKey partner) {
		Intent intent = plugin.createIntent();

		SharedResultReceiver resultReceiver = new SharedResultReceiver(new SharedResultReceiver.Callback() { @Override public void call(Bundle bundle) {
			try {
				bundle.setClassLoader(context.getClassLoader());
				String text = bundle.getString(LABEL);
				byte[] jpegImage = bundle.getByteArray(JPEG_IMAGE);
				LogUtils.info(SingleMessageSession.class, "Receiving message of type '" + plugin.tupleType() + "' text '" + text + "' jpeg-image " + jpegImage + "' from " + plugin);
				sneer.tupleSpace().publisher()
					.field("message-type", plugin.tupleType())
					.type("message")
					.audience(partner)
					.field(LABEL, text)
					.field(JPEG_IMAGE, jpegImage)
					.pub(getPayload(bundle));
			} catch (final Throwable t) {
				AndroidUtils.toastOnMainThread(context, "Error receiving message from plugin: " + plugin, Toast.LENGTH_LONG);
				LogUtils.error(SneerAndroidImpl.class, "Error receiving message from plugin: " + plugin, t);
			}
		}});

		intent.putExtra(RESULT_RECEIVER, resultReceiver);
		context.startActivity(intent);
	}


	static private Object getPayload(Bundle bundle) {
		return ((Value)bundle.getParcelable(PAYLOAD)).get();
	}

}
