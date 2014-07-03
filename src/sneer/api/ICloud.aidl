package sneer.api;

import android.os.Parcel;
import android.os.Bundle;
import sneer.api.ISubscriber;
import sneer.api.ISubscription;
import sneer.api.Value;

interface ICloud {
	oneway void pubPath(in Value[] path);	
	oneway void pubValue(in Value[] path, in Value value);	
	ISubscription sub(in Value[] path, in ISubscriber subscriber);
	byte[] ownPublicKey();
}