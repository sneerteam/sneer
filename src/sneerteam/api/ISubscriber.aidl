package sneerteam.api;

import android.os.Bundle;
import android.net.Uri;

interface ISubscriber {
	oneway void on(in Uri path, in Bundle value);
}
