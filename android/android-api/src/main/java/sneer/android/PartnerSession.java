package sneer.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.List;

import sneer.android.impl.SharedResultReceiver;
import sneer.android.impl.Utils;
import sneer.android.impl.Value;

import static sneer.android.impl.IPCProtocol.*;

public class PartnerSession {

	public static PartnerSession join(Context context, Intent intent, Listener listener) {
		return null;
	}

    public interface Listener {
		void onHistoryReplay(List<Message> history);
		void onNewMessage(Message message);
    }

    private final Listener listener;
    private final ClassLoader classLoader;
    private final ResultReceiver toSneer;
    private boolean isReplaying;


    public PartnerSession(final Context context, final Intent intent, final Listener listener) {
        this.listener = listener;
        this.classLoader = context.getClassLoader();
        toSneer = resultReceiver(intent);
        toSneer.send(0, hail());
    }


    private Bundle hail() {
        Bundle hail = new Bundle();
        hail.putParcelable(RESULT_RECEIVER, new SharedResultReceiver(new SharedResultReceiver.Callback() { @Override public void call(Bundle received) {
            received.setClassLoader(classLoader);
            handlePartnerName(received);
            handlePayload(received);
            handleRefresh(received);
        }}));
        return hail;
    }

    private void handlePartnerName(Bundle received) {
        String partnerName = received.getString(PARTNER_NAME);
		/*
        if (partnerName != null)
            listener.onPartnerName(partnerName);
        */
    }

    private void handlePayload(Bundle received) {
        Object messageEnvelope = received.get(PAYLOAD);
        if (messageEnvelope == null) return;

        Object message = ((Value)messageEnvelope).get();
        boolean mine = received.getBoolean(OWN);
		/*
        if (mine)
           listener.onMessageToPartner(message);
        else
           listener.onMessageFromPartner(message);
        */
    }

    private void handleRefresh(Bundle received) {
        if (received.getBoolean(REPLAY_FINISHED))
            isReplaying = false;

		/*
        if (!isReplaying)
            listener.refresh();
        */
    }

    private ResultReceiver resultReceiver(Intent intent) {
        return Utils.getExtra(intent, RESULT_RECEIVER);
    }


    public void dispose() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(UNSUBSCRIBE, true);
        toSneer.send(0, bundle);
    }

    public void send(String label, Object message) {
        Messages.send(toSneer, label, message, null);
    }
}
