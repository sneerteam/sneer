package sneer.android.main.ipc;

import static sneer.SneerAndroidClient.LABEL;
import static sneer.SneerAndroidClient.MESSAGE;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.android.main.SneerAndroidCore;
import sneer.android.main.utils.AndroidUtils;
import sneer.tuples.Tuple;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
		
		intent.putExtra(MESSAGE, Value.of(tuple.payload()));
		intent.putExtra(LABEL, (String)tuple.get("label"));

		return intent;
	}
	

	@Override
	public void startNewSessionWith(final PublicKey partner) {
		Intent intent = plugin.createIntent();
		
		SharedResultReceiver resultReceiver = new SharedResultReceiver(new SharedResultReceiver.Callback() {  @Override public void call(Bundle t1) {
			
			try {
				t1.setClassLoader(context.getClassLoader());
				Object message = ((Value)t1.getParcelable(MESSAGE)).get();
				String label = t1.getString(LABEL);
				info("Receiving message of type '" + plugin.tupleType() + "' label '" + label + "' from " + plugin);
				sneer.tupleSpace().publisher()
					.type(plugin.tupleType())
					.audience(partner)
					.field("conversation?", true)
					.field("label", label)
					.pub(message);
			} catch (final Throwable t) {
				AndroidUtils.toastOnMainThread(context, "Error receiving message from plugin: " + t.getMessage(), Toast.LENGTH_LONG);
				Log.e(SneerAndroidCore.class.getSimpleName(), "Error receiving message from plugin", t);
			}			
		}});
		
		intent.putExtra(RESULT_RECEIVER, resultReceiver);
		context.startActivity(intent);
	}
	

	protected void info(String string) {
		Log.i(SingleMessageSession.class.getSimpleName(), string);
	}

}
