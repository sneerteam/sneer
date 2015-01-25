package sneer.android.ui;

import android.os.Bundle;

public abstract class PartnerSessionActivity extends SneerActivity {

	private PartnerMessenger toSneer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        toSneer = new PartnerMessenger(this, new PartnerMessenger.Listener() {
            @Override
            public void onPartnerName(String name) {
                PartnerSessionActivity.this.onPartnerName(name);
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
                PartnerSessionActivity.this.update();
            }
        });
	}


	@Override
	protected void onDestroy() {
		if (toSneer != null) {
			toSneer.dispose();
		}
		super.onDestroy();
	}
	

	/** Called in the Android main thread (UI thread).
	 *  @param name The current name of the peer with you in this session. */
	protected void onPartnerName(String name) {};

    protected void send(String label, Object message) {
        toSneer.send(label, message);
    }

    protected abstract void onMessageToPartner(Object message);

	protected abstract void onMessageFromPartner(Object message);

	/**
	 * Called in the Android main thread (UI thread) after each message, if it is the most recent message in the session. This method will
	 * not be called, therefore, when previous messages in the session are being replayed.
	 */
	protected abstract void update();

}
