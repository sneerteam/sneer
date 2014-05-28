package sneerteam.snapi;

import rx.*;
import rx.subjects.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class ContactPicker {

	private static final int PICK_CONTACT_REQUEST = 100;
	private Subject<String, String> contact = ReplaySubject.create();

	public Observable<String> pickContact(Activity activity) {
		Intent intent = new Intent("sneerteam.intent.action.PICK_CONTACT");
		activity.startActivityForResult(intent, PICK_CONTACT_REQUEST);
		return contact;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == PICK_CONTACT_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				Bundle extras = intent.getExtras();
				contact.onNext(extras.get("public_key").toString());
			}
		}
	}
}
