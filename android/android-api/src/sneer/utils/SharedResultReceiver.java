package sneer.utils;

import android.os.Bundle;
import android.os.ResultReceiver;

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
