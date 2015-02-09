package sneer.android.ui;

import android.app.Activity;
import android.os.Bundle;

import sneer.android.PartnerSession;

public abstract class PartnerSessionActivity extends Activity {

	private PartnerSession toPartner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        toPartner = new PartnerSession(this.getApplicationContext(), getIntent(), new PartnerSession.Listener() {
            @Override
            public void onPartnerName(final String name) {
                runOnUiThread(new Runnable() { @Override public void run() {
                    PartnerSessionActivity.this.onPartnerName(name);
                }});
            }

            @Override
            public void onMessageToPartner(Object message) {
                PartnerSessionActivity.this.onMessageToPartner(message);
            }

            @Override
            public void onMessageFromPartner(Object message) {
                PartnerSessionActivity.this.onMessageFromPartner(message);
            }

            @Override
            public void update() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PartnerSessionActivity.this.update();
                    }
                });
            }
        });
	}


	@Override
	protected void onDestroy() {
		if (toPartner != null)
			toPartner.dispose();
		super.onDestroy();
	}
	

	/** Called in the Android main thread (UI thread).
	 *  @param name The current name of the peer with you in this session. */
	protected void onPartnerName(String name) {}

    protected void send(String label, Object message) {
        toPartner.send(label, message);
    }

    protected abstract void onMessageToPartner(Object message);

	protected abstract void onMessageFromPartner(Object message);

	/**
	 * Called in the Android main thread (UI thread) after each message, if it is the most recent message in the session. This method will
	 * not be called, therefore, when previous messages in the session are being replayed.
	 */
	protected abstract void update();

}
