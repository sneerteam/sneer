package sneer.android.ipc;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import sneer.Contact;
import sneer.Conversation;
import sneer.Party;
import sneer.PublicKey;

import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.SneerAndroidSingleton.sneerAndroid;

public class SendMessage extends IntentService {

	public static final String TOKEN = "TOKEN";

	public SendMessage() {
        super("SendMessage");
    }


	static Intent intentFor(Conversation convo) {
		return new Intent()
			.setClassName("me.sneer", SendMessage.class.getName())
			.putExtra(TOKEN, convo.contact().party().current().publicKey().current().toHex());
	}


    @Override
    protected void onHandleIntent(Intent intent) {
		if (intent == null) return;
		try {
			tryToHandle(intent);
		} catch (RuntimeException x) {
			Log.d(getClass().getName(), "Unable to send message", x);
        }
    }


	private void tryToHandle(Intent intent) {
		Log.d(getClass().getName(), "Intent received");
		String pukHex  = intent.getStringExtra(TOKEN);
		String message = intent.getAction();

		PublicKey puk = sneerAndroid().admin().keys().createPublicKey(pukHex);
		Party party = sneer().produceParty(puk);
		Contact contact = sneer().findContact(party);

		sneer().conversations().withContact(contact).sendMessage(message);
	}

}
