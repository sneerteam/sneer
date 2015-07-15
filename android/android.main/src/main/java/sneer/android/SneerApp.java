package sneer.android;

import android.content.Context;
import android.os.StrictMode;
import android.support.multidex.MultiDexApplication;

import java.io.File;

import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.gcm.GcmRegistrationAlarmReceiver;
import sneer.android.impl.SneerAndroidImpl;
import sneer.android.impl.SneerAndroidImplOld;
import sneer.android.ipc.PartnerSessions;
import sneer.android.ui.Notifier;
import sneer.android.utils.UncaughtExceptionReporter;
import sneer.commons.PersistenceFolder;
import sneer.commons.Startup;

import static sneer.android.SneerAndroidContainer.container;

public class SneerApp extends MultiDexApplication {

	private static final boolean DEVELOPER_MODE = false;

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEVELOPER_MODE) setStrictMode();

		Context app = getApplicationContext();
		UncaughtExceptionReporter.start(app, "klauswuestefeld@gmail.com", "Sneer");

		// TODO: Migrate to container.
		// Old pre-container way:
		Notifier.start(app);
		SneerAndroidSingleton.setInstance(new SneerAndroidImpl(app));
		SneerAndroidSingletonOld.setInstance(new SneerAndroidImplOld(app));

		if (SneerAndroidSingleton.admin() == null)
			System.out.println("CORE NOT FOUND.");
		else {
			PartnerSessions.init(SneerAndroidSingleton.sneer().conversations());
			GcmRegistrationAlarmReceiver.schedule(app);

			// New container way:
			SneerAdmin admin = SneerAndroidSingleton.admin();
			container().inject(SneerAdmin.class, admin);
			container().inject(Sneer.class, admin.sneer());
			container().inject(PersistenceFolder.class, new PersistenceFolder() {
				public File get() {
					return getFilesDir();
				}
			});
			container().produce(Startup.class);
		}
	}

	private void setStrictMode() {
		StrictMode.setThreadPolicy(
				new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
		StrictMode.setVmPolicy(
				new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
	}

}
