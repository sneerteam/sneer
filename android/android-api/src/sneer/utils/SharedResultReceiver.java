package sneer.utils;

import rx.functions.*;
import android.os.*;

public class SharedResultReceiver extends ResultReceiver {

	private final transient Action1<Bundle> callback;

	public SharedResultReceiver(Action1<Bundle> callback) {
		super(null);
		this.callback = callback;
	}
	
	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		callback.call(resultData);
	}

}
