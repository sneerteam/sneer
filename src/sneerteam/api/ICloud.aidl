package sneerteam.api;

import android.os.Parcel;
import android.os.Bundle;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;
import sneerteam.api.Value;

interface ICloud {
	oneway void pubPath(in Value[] path);	
	oneway void pubValue(in Value[] path, in Value value);	
	ISubscription sub(in Value[] path, in ISubscriber subscriber);
	byte[] ownPublicKey();
	oneway void registerForNotification(in Value[] segment, in Intent intent);
}