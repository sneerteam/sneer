package sneerteam.snapi;

import android.app.Activity;
import android.content.Intent;

public class ContactPicker {

	public static void startActivityForResult(Activity caller, int requestCode) {
		Intent intent = new Intent("sneerteam.intent.action.PICK_CONTACT");
		caller.startActivityForResult(intent, requestCode);
	}
	
	public static String publicKeyFrom(Intent intent) {
		return intent.getExtras().get("public_key").toString();
	}
}
