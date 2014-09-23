package sneer.android.main.ipc;

import sneer.PublicKey;
import sneer.tuples.Tuple;

public interface PluginSession {

	void resume(Tuple tuple);

	void start(PublicKey partner);

}
