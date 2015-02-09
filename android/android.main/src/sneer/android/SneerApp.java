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
import sneer.android.impl.SneerAndroidImpl;
import sneer.commons.Streams;

import java.io.IOException;
import java.io.InputStream;

public class SneerApp extends Application {

    public static final String PROPERTY_REG_ID = "registration_id";

    private static final String PROPERTY_APP_VERSION = "appVersion";

    private static final String SENDER_ID = "670346118517";


    @Override
	public void onCreate() {
		super.onCreate();
//		SneerAndroidSingleton.setInstance(isCoreAvailable()
//			? new SneerAndroidImpl(getApplicationContext())
//			: new SneerAndroidSimulator(getApplicationContext()));

        SneerAndroidSingleton.setInstance(new SneerAndroidImpl(getApplicationContext()));

        String registrationId = getRegistrationId(getApplicationContext());
        if (registrationId.isEmpty())
            registerInBackground();
        else
            sendRegistrationIdToBackendInBackground(registrationId);
	}

    private void registerInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    String gcmId = gcm.register(SENDER_ID);
                    log("Device registered, registration ID=" + gcmId);

                    storeRegistrationId(getApplicationContext(), gcmId);

                    sendRegistrationIdToBackend(gcmId);

                } catch (IOException ex) {
                    String msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    log(msg);
                }
                return null;
            }

        }.execute();
    }

    private void sendRegistrationIdToBackendInBackground(final String registrationId) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                sendRegistrationIdToBackend(registrationId);
                return null;
            }
        }.execute();
    }

    private void log(String msg) {
        Log.d(getClass().getName(), msg);
    }

    private void sendRegistrationIdToBackend(String gcmId) {
        AndroidHttpClient client = AndroidHttpClient.newInstance("sneer.android.main", getApplicationContext());
        try {
            log("GCM: sending id to server...");
            HttpResponse response = client.execute(new HttpGet(registrationUriFor(gcmId)));
            InputStream content = response.getEntity().getContent();
            log("GCM: server registration response: (" + response.getStatusLine() + ") - " + readString(content));
        } catch (IOException e) {
            e.printStackTrace();
            log("GCM: failed to send registration (exception was reported)");
        } finally {
            client.close();
        }
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


    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(getClass().getName(), "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(getClass().getName(), "App version changed.");
            return "";
        }
        Log.d(getClass().getName(), "Registration loaded: " + registrationId + " <<<<<");
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(getClass().getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(getClass().getName(), "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    // APA91bGdLsycUvLKLaWXw807-USTEBUcYE6p-D_758maR_iNQJpu_cb9-4NPDPe0HXimtGlZYLYACNsd_Imb5hQsFspdpJLOIhWknbxh1T7dqVimNr2pLoF_7_iya5RuYO5ksddp7YUjB7C_0n4bXzgDSJzf7tVtog

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
