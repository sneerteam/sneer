package sneerteam.snapi;

import sneerteam.api.ICloud;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

class ICloudImpl extends ICloud.Stub {

	public void close() {
		int implementMe;
		
	}

	@Override
	public void pubPath(Uri path) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pubValue(Uri path, Bundle value) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ISubscription sub(Uri path, ISubscriber subscriber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
