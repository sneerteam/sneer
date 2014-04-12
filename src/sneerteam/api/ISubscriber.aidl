package sneerteam.api;

import android.os.Bundle;

interface ISubscriber {
	oneway void onPath(in Bundle[] path);
	oneway void onValue(in Bundle[] path, in Bundle value);
}
