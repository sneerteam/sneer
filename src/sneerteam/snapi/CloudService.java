package sneerteam.snapi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CloudService extends Service {
	
	private CloudMaster master;

	@Override public void onCreate()  { super.onCreate(); master = new CloudMasterImpl(); };
	@Override public void onDestroy() {	if (master != null) master.close();	super.onDestroy(); }
	
	@Override
	public IBinder onBind(Intent intent) {
		log("onBind(" + intent.getAction() + ")");
		Object appId = System.currentTimeMillis(); int RodrigoWeHowDoWeFindAnIdForTheCallingApp;
		return master.freshCloudFor(appId);
	}
	
	private void log(String message) {
		Log.d(getClass().getCanonicalName(), message);
	}
}
