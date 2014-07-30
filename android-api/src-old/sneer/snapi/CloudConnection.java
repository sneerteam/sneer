package sneer.snapi;

import java.util.*;

import rx.*;
import rx.schedulers.*;
import sneer.api.*;
import android.os.*;

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

	public Scheduler scheduler() {
		return scheduler;
	}

}
