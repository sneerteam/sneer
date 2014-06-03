package sneerteam.snapi;

import android.app.Activity;
import android.content.Intent;

public class ContactPicker {

	public static void startActivityForResult(Activity caller, int requestCode) {
		Intent intent = new Intent("sneerteam.intent.action.PICK_CONTACT");
		caller.startActivityForResult(intent, requestCode);
	}
	
	public static Contact contactFrom(Intent intent) {
		return new Contact(intent.getExtras().get("public_key").toString(), intent.getExtras().get("nickname").toString());
	}
}
