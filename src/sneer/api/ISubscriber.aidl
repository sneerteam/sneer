package sneer.api;

import android.os.Bundle;
import sneer.api.Value;

interface ISubscriber {
	oneway void onPath(in Value[] path);
	oneway void onValue(in Value[] path, in Value value);
}
