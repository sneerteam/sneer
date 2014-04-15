package sneerteam.snapi;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import sneerteam.api.ICloud;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;
import sneerteam.api.Value;

public class CloudConnection {
	
	final ICloud cloud;
	final boolean remote;

	public CloudConnection(ICloud cloud) {
		this(cloud, false);
	}
	
	public CloudConnection(IBinder binder) {
		this(ICloud.Stub.asInterface(binder), true);
	}
	
	public CloudConnection(ICloud cloud, boolean remote) {
		this.cloud = cloud;
		this.remote = remote;
	}

	public Path path(Object... segments) {
		return path(Arrays.asList(segments));
	}
	
	public Path path(List<Object> segments) {
		return new Path(this, segments);
	}	

	ISubscription sub(Value[] path, ISubscriber subscriber) throws RemoteException {
		return cloud.sub(path, remote ? stub(subscriber) : subscriber);
	}

	ISubscriber stub(final ISubscriber subscriber) {
		return new ISubscriber.Stub() {
			@Override
			public void onValue(Value[] path, Value value) throws RemoteException {
				subscriber.onValue(path, value);
			}
			
			@Override
			public void onPath(Value[] path) throws RemoteException {
				subscriber.onPath(path);
			}
		};
	}

	public byte[] ownPublicKey() throws RemoteException {
		return cloud.ownPublicKey();
	}
	
	
}
