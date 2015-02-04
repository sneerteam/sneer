package sneer.android.impl;

import android.os.Bundle;
import android.os.ResultReceiver;

/**
 *
 *
 * ==========================================================
 * Using this class solves ResultReceiver classloader issues.
 * The specific ResultReceiver subclass used must be loadable
 * on both sides of the IPC. Just using annonymous inner
 * subclasses of ResultReceiver, as its javadoc suggests, is
 * not a good idea.
 * ==========================================================
 *
 *
 *
 * */
public class SharedResultReceiver extends ResultReceiver {

	private final transient Callback callback;
	
	
	public interface Callback {
		void call(Bundle resultData);
	}

	
	public SharedResultReceiver(Callback callback) {
		super(null);
		this.callback = callback;
	}
	
	
	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		callback.call(resultData);
	}

}
