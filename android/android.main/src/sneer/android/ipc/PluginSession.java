package sneer.android.ipc;

import sneer.PublicKey;
import sneer.tuples.Tuple;
import android.content.Intent;

public interface PluginSession {

	Intent createResumeIntent(Tuple tuple);

	void startNewSessionWith(PublicKey partner);

}
