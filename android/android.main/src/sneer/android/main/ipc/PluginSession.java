package sneer.android.main.ipc;

import android.content.Intent;
import sneer.PublicKey;
import sneer.tuples.Tuple;

public interface PluginSession {

	Intent createResumeIntent(Tuple tuple);

	void startNewSessionWith(PublicKey partner);

}
