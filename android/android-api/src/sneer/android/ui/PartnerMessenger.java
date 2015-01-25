package sneer.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.ResultReceiver;

import sneer.SneerAndroidClient;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;

import static sneer.SneerAndroidClient.OWN;
import static sneer.SneerAndroidClient.PARTNER_NAME;
import static sneer.SneerAndroidClient.PAYLOAD;
import static sneer.SneerAndroidClient.REPLAY_FINISHED;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import static sneer.SneerAndroidClient.UNSUBSCRIBE;

public class PartnerMessenger {

    public interface Listener {

        /** Called in the Android main thread (UI thread).
         *  @param name The current name of the peer with you in this session. */
        void onPartnerName(String name);

        void onMessageToPartner(Object message);

        void onMessageFromPartner(Object message);

        /**
         * Called in the Android main thread (UI thread) after each message, if it is the most recent message in the session. This method will
         * not be called, therefore, when previous messages in the session are being replayed.
         */
        void update();
    }

    private boolean isReplaying;
    private ResultReceiver toSneer;

    public PartnerMessenger(final Activity a, final Listener session) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(RESULT_RECEIVER, new SharedResultReceiver(new SharedResultReceiver.Callback() {  @Override public void call(Bundle data) {

            data.setClassLoader(a.getApplicationContext().getClassLoader());

            final String partnerName = data.getString(PARTNER_NAME);

            if (partnerName != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        session.onPartnerName(partnerName);
                    }
                });
            }

            if (data.getBoolean(REPLAY_FINISHED))
                isReplaying = false;

            Object messageEnvelope = data.get(PAYLOAD);

            if (messageEnvelope != null) {
                Object message = ((Value)messageEnvelope).get();
                boolean mine = data.getBoolean(OWN);

                if (mine)
                    session.onMessageToPartner(message);
                else
                    session.onMessageFromPartner(message);
            }

            if (!isReplaying) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        session.update();
                    }
                });
            }
        }}));

        toSneer = resultReceiver(a);
        toSneer.send(0, bundle);
    }

    private ResultReceiver resultReceiver(Activity a) {
        return getExtra(a, RESULT_RECEIVER);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getExtra(Activity a, String extra) {
        Bundle extras = a.getIntent().getExtras();
        return extras == null ? null : (T)extras.get(extra);
    }

    public void dispose() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(UNSUBSCRIBE, true);
        toSneer.send(0, bundle);
    }

    public void send(String label, Object message) {
        SneerAndroidClient.send(toSneer, label, message, null);
    }
}
