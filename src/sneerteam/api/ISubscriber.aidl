package sneerteam.api;

import android.os.Bundle;
import sneerteam.api.Value;

interface ISubscriber {
	oneway void onPath(in Value[] path);
	oneway void onValue(in Value[] path, in Value value);
}
