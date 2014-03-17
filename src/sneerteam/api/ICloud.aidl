package sneerteam.api;

import android.os.Bundle;
import android.net.Uri;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;

interface ICloud {
	oneway void pub(in Uri path, in Bundle value);	
	ISubscription sub(in Uri path, in ISubscriber subscriber);
}