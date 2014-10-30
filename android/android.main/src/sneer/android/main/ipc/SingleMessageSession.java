package sneer.android.main.ipc;

import static sneer.SneerAndroidClient.TEXT;
import static sneer.SneerAndroidClient.PAYLOAD;
import static sneer.SneerAndroidClient.JPEG_IMAGE;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.android.main.SneerAndroidCore;
import sneer.android.main.utils.AndroidUtils;
import sneer.android.main.utils.LogUtils;
import sneer.tuples.Tuple;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class SingleMessageSession implements PluginSession {

	private Context context;
	private Sneer sneer;
	private PluginHandler plugin;


	SingleMessageSession(Context context, Sneer sneer, PluginHandler plugin) {
		this.context = context;
		this.sneer = sneer;
		this.plugin = plugin;
	}
	

	@Override
	public Intent createResumeIntent(Tuple tuple) {
		Intent intent = plugin.createIntent();
		
		intent.putExtra(PAYLOAD, Value.of(tuple.payload()));
		intent.putExtra(TEXT, (String)tuple.get(TEXT));
		intent.putExtra(JPEG_IMAGE, (String)tuple.get(JPEG_IMAGE));

		return intent;
	}
	

	@Override
	public void startNewSessionWith(final PublicKey partner) {
		Intent intent = plugin.createIntent();
		
		SharedResultReceiver resultReceiver = new SharedResultReceiver(new SharedResultReceiver.Callback() { @Override public void call(Bundle bundle) {			
			try {
				bundle.setClassLoader(context.getClassLoader());
				String text = bundle.getString(TEXT);
				byte[] jpegImage = bundle.getByteArray(JPEG_IMAGE);
				LogUtils.info(SingleMessageSession.class, "Receiving message of type '" + plugin.tupleType() + "' text '" + text + "' jpeg-image " + jpegImage + "' from " + plugin);
				sneer.tupleSpace().publisher()
					.field("message-type", plugin.tupleType())
					.type("message")
					.audience(partner)
					.field(TEXT, text)
					.field(JPEG_IMAGE, jpegImage)
					.pub(getPayload(bundle));
			} catch (final Throwable t) {
				AndroidUtils.toastOnMainThread(context, "Error receiving message from plugin: " + plugin, Toast.LENGTH_LONG);
				LogUtils.error(SneerAndroidCore.class, "Error receiving message from plugin: " + plugin, t);
			}			
		}});
		
		intent.putExtra(RESULT_RECEIVER, resultReceiver);
		context.startActivity(intent);
	}


	static private Object getPayload(Bundle bundle) {
		return ((Value)bundle.getParcelable(PAYLOAD)).get();
	}
	
}
