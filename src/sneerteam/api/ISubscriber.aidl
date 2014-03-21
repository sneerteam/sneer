package sneerteam.api;

import android.os.Bundle;
import android.net.Uri;

interface ISubscriber {
	oneway void onPath(in Uri path);
	oneway void onValue(in Uri path, in Bundle value);
}
