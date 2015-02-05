//package sneer.android.ui;
//
//import sneer.Message;
//
//import android.app.Activity;
//import android.os.Bundle;
//
//public abstract class GroupSessionActivity extends Activity {
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		// TODO: Start replaying previous messages on separate thread.
//	}
//
//
//	/** TODO: Who is in the group? Use Party? Use RX? */
//
//
//	protected void send(String label, Object messageContents) { /*TODO*/ }
//	protected abstract void onMessage(Message message);
//
//
//	/**
//	 * Called in the Android main thread (UI thread) after each message, if it is the most recent message in the session. This method will
//	 * not be called, therefore, when previous messages in the session are being replayed.
//	 */
//	protected abstract void update();
//
//}
