package sneer.android.gcm.tests;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import sneer.android.gcm.RegistrationController;

import java.util.ArrayList;

public class RegistrationControllerTest extends AndroidTestCase {

    public void testWhenUnregisteredWillRegisterAndNotifyBackend() throws Exception {
        SharedPreferences prefs = sharedPrefs();

        new RegistrationController(prefs, appVersion(1), successfulGcmClient("42"), successfulBackendClient()).run();

        assertEquals("42", prefs.getString("id", ""));
        assertEquals(1, prefs.getInt("registered-app-version", 0));
        assertIdSentToBackend("42");
    }

    public void testWhenRegisteredWillNotRegisterButWillNotifyBackend() throws Exception {
        SharedPreferences prefs = sharedPrefs();
        prefs.edit()
            .putString("id", "42")
            .putInt("registered-app-version", 1)
            .apply();

        new RegistrationController(prefs, appVersion(1), failingGcmClient(), successfulBackendClient()).run();

        assertEquals("42", prefs.getString("id", ""));
        assertIdSentToBackend("42");
    }

    public void testWhenRegisteredAndAppVersionChangesWillRegisterAgain() throws Exception {
        SharedPreferences prefs = sharedPrefs();
        prefs.edit()
            .putString("id", "42")
            .putInt("registered-app-version", 1)
            .apply();

        String newId = "43";
        new RegistrationController(prefs, appVersion(2), successfulGcmClient(newId), successfulBackendClient()).run();

        assertEquals(newId, prefs.getString("id", ""));
        assertEquals(2, prefs.getInt("registered-app-version", 0));
        assertIdSentToBackend("43");
    }

    private RegistrationController.GcmClient failingGcmClient() {
        return new RegistrationController.GcmClient() {
            @Override
            public String register() {
                fail("unexpected registration");
                return null;
            }
        };
    }

    private RegistrationController.GcmClient successfulGcmClient(final String id) {
        return new RegistrationController.GcmClient() {
            @Override
            public String register() {
                return id;
            }
        };
    }

    private RegistrationController.BackendClient successfulBackendClient() {
        return new RegistrationController.BackendClient() {
            @Override
            public void register(String gcmId) {
                idsSentToBackend.add(gcmId);
            }
        };
    }

    private void assertIdSentToBackend(String expected) {
        assertEquals(1, idsSentToBackend.size());
        assertEquals(expected, idsSentToBackend.get(0));
    }

    private SharedPreferences sharedPrefs() {
        SharedPreferences prefs = getContext().getSharedPreferences(getClass().getName(), Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        return prefs;
    }

    private RegistrationController.AppVersionProvider appVersion(final int version) {
        return new RegistrationController.AppVersionProvider() {
            @Override
            public int appVersion() {
                return version;
            }
        };
    }

    final ArrayList<String> idsSentToBackend = new ArrayList<>();
}
