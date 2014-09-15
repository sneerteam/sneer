package sneer.android.ui;

import sneer.*;
import sneer.utils.*;
import android.os.*;

public abstract class MessageActivity extends SneerActivity {

	protected Object message() {
		return ((Value)getIntent().getParcelableExtra(SneerAndroidClient.MESSAGE)).get();
	}
	
	protected void send(Object... messages) {
		ResultReceiver resultReceiver = getExtra(SneerAndroidClient.RESULT_RECEIVER);

		Bundle bundle = new Bundle();
		bundle.putParcelable("value", Value.of(messages));
		resultReceiver.send(RESULT_OK, bundle);
	}

}