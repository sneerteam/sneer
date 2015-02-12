package sneer.android.gcm;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

public class RegistrationController {

    public static class Prefs {
        public static final String ID = "id";
        public static final String REGISTERED_APP_VERSION = "registered-app-version";
    }

    public interface AppVersionProvider {
        int appVersion();
    }

    public interface GcmClient {
        String register() throws IOException;

    }

    public interface BackendClient {
        void register(String gcmId) throws IOException;
    }

    private final SharedPreferences prefs;
    private final BackendClient backendClient;
    private final AppVersionProvider appVersionProvider;
    private final GcmClient gcmClient;

    public RegistrationController(SharedPreferences prefs, AppVersionProvider appVersionProvider, GcmClient gcmClient, BackendClient backendClient) {
        this.prefs = prefs;
        this.appVersionProvider = appVersionProvider;
        this.gcmClient = gcmClient;
        this.backendClient = backendClient;
    }

    public void run() throws IOException {
        if (requiresRegistration())
            register();
        notifyBackend();
    }

    private void register() throws IOException {
        String id = gcmClient.register();
        log("Device registered. GCM registration ID is " + id);
        store(id);
    }

    private void notifyBackend() throws IOException {
        backendClient.register(registrationId());
    }

    private void store(String id) {
        prefs.edit()
            .putString(Prefs.ID, id)
            .putInt(Prefs.REGISTERED_APP_VERSION, appVersion())
            .apply();
    }

    private boolean requiresRegistration() {
        if (registrationId().isEmpty()) {
            log("Registration not found.");
            return true;
        }

        if (registeredAppVersion() != appVersion()) {
            log("App version changed.");
            return true;
        }
        return false;
    }

    private int registeredAppVersion() {
        return prefs.getInt(Prefs.REGISTERED_APP_VERSION, Integer.MIN_VALUE);
    }

    private int appVersion() {
        return appVersionProvider.appVersion();
    }

    private String registrationId() {
        return prefs.getString(Prefs.ID, "");
    }

    private int log(String msg) {
        return Log.i(getClass().getName(), msg);
    }
}
