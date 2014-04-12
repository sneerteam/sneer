package sneerteam.api;

import android.os.Parcel;
import android.os.Bundle;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;

interface ICloud {
	oneway void pubPath(in Bundle[] path);	
	oneway void pubValue(in Bundle[] path, in Bundle value);	
	ISubscription sub(in Bundle[] path, in ISubscriber subscriber);
	byte[] ownPublicKey();
}