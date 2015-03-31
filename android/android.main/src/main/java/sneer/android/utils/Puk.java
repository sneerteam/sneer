package sneer.android.utils;

import android.app.Activity;
import android.content.Intent;

import sneer.Party;

import static sneer.android.ui.ContactActivity.USE_INVITES;

public class Puk {

	public static void shareOwnPublicKey(Activity activity, Party self, long inviteCode, String receiver) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sneer Public Key");
		sharingIntent.putExtra(Intent.EXTRA_TEXT,
				"\n\nIf you don't have the Sneer app, install it using the Play Store: https://play.google.com/store/apps/details?id=me.sneer\n\n" +
				"Then, tap to add me as a Sneer contact: " +
				buildSneerUri(self.publicKey().current().toHex(), inviteCode));

		String title = (USE_INVITES ? "Send invite to " : "Share your Public-key with ") + receiver;
		Intent chooser = Intent.createChooser(sharingIntent, title);

		if (sharingIntent.resolveActivity(activity.getPackageManager()) != null)
		    activity.startActivity(chooser);
	}


	public static String buildSneerUri(String puk, long inviteCode) {
		String invite = "&invite=" + inviteCode;
		return "http://sneer.me/public-key?" + puk + (USE_INVITES ? invite : "");
	}

}
