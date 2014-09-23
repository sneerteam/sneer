package sneer.android.main.ipc;

import static sneer.SneerAndroidClient.LABEL;
import static sneer.SneerAndroidClient.MESSAGE;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.android.main.SneerAndroidCore;
import sneer.tuples.Tuple;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SingleMessageSession implements PluginSession {

	public static PluginSessionFactory factory = new PluginSessionFactory() {  @Override public PluginSession create(Context context, Sneer sneer, PluginInfo plugin, SessionIdDispenser session) {
		return new SingleMessageSession(context, sneer, plugin, session);
	} };
	
	private Context context;
	private Sneer sneer;
	private PluginInfo plugin;

	public SingleMessageSession(Context context, Sneer sneer, PluginInfo plugin, SessionIdDispenser session) {
		this.context = context;
		this.sneer = sneer;
		this.plugin = plugin;
	}

	@Override
	public void resume(Tuple tuple) {
		Intent intent = new Intent();
		
		intent.putExtra(MESSAGE, Value.of(tuple.payload()));
		intent.putExtra(LABEL, (String)tuple.get("label"));
		
		plugin.start(context, intent);
	}

	@Override
	public void start(final PublicKey partner) {
		Intent intent = new Intent();
		
		SharedResultReceiver resultReceiver = new SharedResultReceiver(new SharedResultReceiver.Callback() {  @Override public void call(Bundle t1) {
			
			try {
				t1.setClassLoader(context.getClassLoader());
				Object message = ((Value)t1.getParcelable(MESSAGE)).get();
				String label = t1.getString(LABEL);
				info("Receiving message of type '" + plugin.tupleType() + "' label '" + label + "' from " + plugin);
				sneer.tupleSpace().publisher()
					.type(plugin.tupleType())
					.audience(partner)
					.field("label", label)
					.pub(message);
			} catch (final Throwable t) {
				SneerAndroidCore.toastOnMainThread(context, "Error receiving message from plugin: " + t.getMessage(), Toast.LENGTH_LONG);
				Log.e(SneerAndroidCore.class.getSimpleName(), "Error receiving message from plugin", t);
			}			
		}});
		
		intent.putExtra(RESULT_RECEIVER, resultReceiver);
		plugin.start(context, intent);
	}

	protected void info(String string) {
		Log.i(SingleMessageSession.class.getSimpleName(), string);
	}

}
