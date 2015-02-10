package sneer.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.admin.SneerAdmin;
import sneer.android.gcm.RegistrationController;
import sneer.android.impl.SneerAndroidImpl;
import sneer.commons.Streams;

import java.io.IOException;
import java.io.InputStream;

public class SneerApp extends Application {

    private static final String SENDER_ID = "670346118517";


    @Override
	public void onCreate() {
		super.onCreate();
//		SneerAndroidSingleton.setInstance(isCoreAvailable()
//			? new SneerAndroidImpl(getApplicationContext())
//			: new SneerAndroidSimulator(getApplicationContext()));

        SneerAndroidSingleton.setInstance(new SneerAndroidImpl(getApplicationContext()));

        registerInBackground();
	}

    private void registerInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    new RegistrationController(getGCMPreferences(getApplicationContext()), appVersionProvider(), gcmClient(), backendClient()).run();
                } catch (IOException ex) {
                    String msg = "Error during registration: " + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    log(msg);
                }
                return null;
            }

        }.execute();
    }

    private RegistrationController.BackendClient backendClient() {
        return new RegistrationController.BackendClient() {
            @Override
            public void register(String gcmId) throws IOException {
                AndroidHttpClient client = AndroidHttpClient.newInstance("sneer.android.main", getApplicationContext());
                try {
                    log("GCM: sending id to server...");
                    HttpResponse response = client.execute(new HttpGet(registrationUriFor(gcmId)));
                    InputStream content = response.getEntity().getContent();
                    log("GCM: server registration response: (" + response.getStatusLine() + ") - " + readString(content));
                } finally {
                    client.close();
                }
            }
        };
    }

    private RegistrationController.GcmClient gcmClient() {
        return new RegistrationController.GcmClient() {
            @Override
            public String register() throws IOException {
                GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                return gcm.register(SneerApp.SENDER_ID);
            }
        };
    }

    private RegistrationController.AppVersionProvider appVersionProvider() {
        return new RegistrationController.AppVersionProvider() { @Override public int appVersion() {
            return getAppVersion(getApplicationContext());
        }};
    }

    private void log(String msg) {
        Log.d(getClass().getName(), msg);
    }

    private String readString(InputStream content) throws IOException {
        return new String(Streams.readFully(content));
    }

    private String registrationUriFor(String gcmId) {
        return "http://dynamic.sneer.me/gcm/register?id=" + Uri.encode(gcmId) + "&puk=" + Uri.encode(puk().toHex());
    }

    private PublicKey puk() {
        return prik().publicKey();
    }

    private PrivateKey prik() {
        return admin().privateKey();
    }

    private SneerAdmin admin() {
        return SneerAndroidSingleton.admin();
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(RegistrationController.class.getName(), Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public void checkPlayServices(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(getClass().getName(), "This device is not supported.");
                activity.finish();
            }
        }
    }
}
