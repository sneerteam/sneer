package sneerteam.snapi;

import rx.Observable;
import rx.subjects.AsyncSubject;
import rx.subjects.Subject;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class CloudServiceConnection {
	
	public static CloudServiceConnection prepare() {
		return new CloudServiceConnection();
	}
	
	final Subject<CloudConnection, CloudConnection> connection = AsyncSubject.create();
	
	final ServiceConnection snapi = new ServiceConnection() {		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			connection.onNext(new CloudConnection(binder));
			connection.onCompleted();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};

	private boolean isBound;
	
	private CloudServiceConnection() {
	}
	
	public Observable<CloudConnection> cloud() {
		return connection;
	}

	public void bind(Context context) {
		if (isBound) throw new IllegalStateException("connection already bound!");
		isBound = true;
		context.bindService(
				bindCloudServiceIntent(),
				snapi,
				Context.BIND_AUTO_CREATE + Context.BIND_ADJUST_WITH_ACTIVITY);
	}

	private Intent bindCloudServiceIntent() {
		Intent bindIntent = new Intent("sneerteam.intent.action.BIND_CLOUD_SERVICE");
		bindIntent.setClassName("sneerteam.android.main", "sneerteam.android.main.CloudService");
		return bindIntent;
	}
}
