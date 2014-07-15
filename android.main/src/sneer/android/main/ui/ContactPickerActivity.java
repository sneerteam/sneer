package sneer.android.main.ui;

import sneer.*;
import android.content.*;
import android.os.*;

public class ContactPickerActivity extends ManagedContactsActivity {

	private ResultReceiver resultReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		resultReceiver = (ResultReceiver)getIntent().getExtras().get("result");
		if (resultReceiver == null) {
			toast(getClass().getSimpleName() + " called without ResultReceiver.");
			finish();
			return;
		}
		
	}

	@Override
	protected void onContactClicked(Contact contact) {
		Intent intent = new Intent();
		intent.putExtra("public_key", contact.party().publicKey().toBlockingObservable().first());

		resultReceiver.send(1, intent.getExtras());
		
		setResult(RESULT_OK, intent);
		finish();
	}
	
}
