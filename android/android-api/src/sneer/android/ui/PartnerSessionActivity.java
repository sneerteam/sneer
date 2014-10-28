package sneer.android.ui;

import static sneer.SneerAndroidClient.OWN;
import static sneer.SneerAndroidClient.PARTNER_NAME;
import static sneer.SneerAndroidClient.PAYLOAD;
import static sneer.SneerAndroidClient.REPLAY_FINISHED;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import static sneer.SneerAndroidClient.UNSUBSCRIBE;
import sneer.SneerAndroidClient;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.os.Bundle;
import android.os.ResultReceiver;

public abstract class PartnerSessionActivity extends SneerActivity {

	private ResultReceiver toSneer;
	private boolean isReplaying = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = new Bundle();
		bundle.putParcelable(RESULT_RECEIVER, new SharedResultReceiver(new SharedResultReceiver.Callback() {  @Override public void call(Bundle data) {
			
			data.setClassLoader(getApplicationContext().getClassLoader());
			
			final String partnerName = data.getString(PARTNER_NAME);
			
			if (partnerName != null) {
				runOnUiThread(new Runnable() { @Override public void run() {
					onPartnerName(partnerName);
				}});
			}
			
			boolean replayFinished = data.getBoolean(REPLAY_FINISHED);
			if (replayFinished) {
				isReplaying = false;
			}
			
			Object messageEnvelope = data.get(PAYLOAD);
			
			if (messageEnvelope != null) {
				Object message = ((Value)messageEnvelope).get();
				boolean mine = data.getBoolean(OWN);
				
				if (mine)
					onMessageToPartner(message);
				else
					onMessageFromPartner(message);				
			}
			
			if (!isReplaying) {
				runOnUiThread(new Runnable() {  @Override public void run() {
					update();
				}});
			}			
		}}));
		
		toSneer = getExtra(RESULT_RECEIVER);
		toSneer.send(0, bundle);
	}
	
	
	@Override
	protected void onDestroy() {		
		if (toSneer != null) {
			Bundle bundle = new Bundle();
			bundle.putBoolean(UNSUBSCRIBE, true);
			toSneer.send(0, bundle);
		}
		
		super.onDestroy();		
	}
	
	

	/** Called in the Android main thread (UI thread).
	 *  @param name The current name of the peer with you in this session. */
	protected void onPartnerName(String name) {};

	
	protected void send(String label, Object message) {
		SneerAndroidClient.send(toSneer, label, message);
	}
	protected abstract void onMessageToPartner(Object message);
	
	protected abstract void onMessageFromPartner(Object message);

	
	/**
	 * Called in the Android main thread (UI thread) after each message, if it is the most recent message in the session. This method will
	 * not be called, therefore, when previous messages in the session are being replayed.
	 */
	protected abstract void update();

}
