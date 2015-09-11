package sneer.android.gcm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.admin.SneerAdmin;
import sneer.android.SneerAndroidSingleton;
import sneer.commons.SystemReport;

import static sneer.commons.Streams.readString;
import static sneer.commons.exceptions.Exceptions.check;

public class GcmRegistrationAlarmReceiver extends BroadcastReceiver {

    private static final String SENDER_ID = "670346118517";
    private static Context appContext;

    public static void schedule(Context appContext_) {
        check(appContext == null);
        appContext = appContext_;
        schedule();
    }

    public static void schedule() {
        Intent newIntent = new Intent(appContext, GcmRegistrationAlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(appContext, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager service = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        service.set(AlarmManager.RTC, oneMinuteFromNow(), pending);
    }

    private static long oneMinuteFromNow() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 1);
        return cal.getTimeInMillis();
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (SneerAndroidSingleton.admin() == null)      // Running without core
            return;
        log("GCM wake up");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                runIn(context);
                return null;
            }
        }.execute();
    }

    private void runIn(Context ignored) {
        try {
            RegistrationControllerHost host = new RegistrationControllerHost();
            new RegistrationController(getGCMPreferences(appContext), host, host, host).run();
            updateReport("Registration successful.");
        } catch (IOException ex) {
            String msg = "Error during registration: " + ex.getMessage();
            // TODO: Consider exponential back-off.
            log(msg);
            updateReport(msg);
            schedule();
        }
    }

    private void updateReport(String info) {
        SystemReport.updateReport("gcm", info);
    }

    SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences("gcm", Context.MODE_PRIVATE);
    }

    void log(String msg) {
        Log.d(getClass().getName(), msg);
    }


    class RegistrationControllerHost implements RegistrationController.AppVersionProvider, RegistrationController.BackendClient, RegistrationController.GcmClient {

        @Override
        public int appVersion() {
            return getAppVersion(appContext);
        }

        @Override
        public void register(String gcmId) throws IOException {
            AndroidHttpClient client = AndroidHttpClient.newInstance("sneer.android.main", appContext);
            try {
                log("GCM: sending id to server...");
                HttpResponse response = client.execute(new HttpGet(registrationUriFor(gcmId)));
                InputStream content = response.getEntity().getContent();
                log("GCM: server registration response: (" + response.getStatusLine() + ") - " + readString(content));
                if (response.getStatusLine().getStatusCode() != 200)
                    throw new IOException("GCM backend registration failed!");
            } finally {
                client.close();
            }
        }

        @Override
        public String register() throws IOException {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(appContext);
            try {
                return gcm.register(SENDER_ID);
            } catch (RuntimeException rx) {
                throw new IOException("Exception thrown by GoogleCloudMessaging.register(): " + rx.getClass() + ": " + rx.getMessage(), rx);
            }
        }

        String registrationUriFor(String gcmId) {
            return "http://dynamic.sneer.me/gcm/register?id=" + Uri.encode(gcmId) + "&puk=" + Uri.encode(puk().toHex());
        }

        PublicKey puk() {
            return prik().publicKey();
        }

        PrivateKey prik() {
            return admin().privateKey();
        }

        SneerAdmin admin() {
            return SneerAndroidSingleton.admin();
        }

        int getAppVersion(Context context) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException("Could not get package name: " + e);
            }
        }
    }

}
