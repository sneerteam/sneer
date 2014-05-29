package sneerteam.snapi;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import rx.*;
import rx.schedulers.*;
import sneerteam.api.ICloud;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;

public class CloudConnection {
	
	final ICloud cloud;
	final boolean remote;
	private Scheduler scheduler;

	public CloudConnection(ICloud cloud) {
		this(cloud, false, Schedulers.immediate());
	}
	
	public CloudConnection(IBinder binder, Scheduler scheduler) {
		this(ICloud.Stub.asInterface(binder), true, scheduler);
	}
	
	public CloudConnection(ICloud cloud, boolean remote, Scheduler scheduler) {
		this.cloud = cloud;
		this.remote = remote;
		this.scheduler = scheduler;
	}

	public Path path(Object... segments) {
		return path(Arrays.asList(segments));
	}
	
	public Path path(List<Object> segments) {
		return new Path(this, segments);
	}	

	ISubscription sub(Bundle[] path, ISubscriber subscriber) throws RemoteException {
		return cloud.sub(path, remote ? stub(subscriber) : subscriber);
	}

	ISubscriber stub(final ISubscriber subscriber) {
		return new ISubscriber.Stub() {
			@Override
			public void onValue(Bundle[] path, Bundle value) throws RemoteException {
				subscriber.onValue(path, value);
			}
			
			@Override
			public void onPath(Bundle[] path) throws RemoteException {
				subscriber.onPath(path);
			}
		};
	}

	public byte[] ownPublicKey() throws RemoteException {
		return cloud.ownPublicKey();
	}

	public Scheduler scheduler() {
		return scheduler;
	}
}
