package sneerteam.snapi;

import java.util.concurrent.CopyOnWriteArrayList;

import sneerteam.api.ICloud;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class CloudService extends Service {
	
	final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<Subscription>();
	
	final ICloud.Stub binder = new ICloud.Stub() {
		
		@Override
		public ISubscription sub(Uri path, ISubscriber subscriber) throws RemoteException {
			Subscription subscription = new Subscription(subscriber);
			subscriptions.add(subscription);
			return subscription.proxy;
		}
		
		@Override
		public void pub(Uri path, Bundle value) throws RemoteException {
			for (Subscription sub : subscriptions) {
				try {
					sub.subscriber.on(path,  value);
				} catch (RemoteException e) {
					subscriptions.remove(sub);
				}
			}
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		log("onBind(" + intent.getAction() + ")");
		return binder;
	}

	class Subscription {
		final ISubscriber subscriber;

		public Subscription(ISubscriber subscriber) {
			this.subscriber = subscriber;
		}

		final ISubscription proxy = new ISubscription.Stub() {
			@Override
			public void dispose() throws RemoteException {
				subscriptions.remove(Subscription.this);
			}
		};
	}
	
	private void log(String message) {
		Log.d(getClass().getCanonicalName(), message);
	}
	
}
