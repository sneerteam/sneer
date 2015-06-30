package sneer.android.utils;

import android.content.Context;
import android.content.Intent;

import sneer.Party;

public class Puk {

	public static void shareOwnPublicKey(Context context, Party self, String inviteCode, String receiver) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Sneer Invite");
		sharingIntent.putExtra(Intent.EXTRA_TEXT,
				"If you don't have the Sneer app, install it using the Play Store: https://play.google.com/store/apps/details?id=sneer.main\n\n" +
				"Then, tap to add me as a Sneer contact: " +
				buildSneerUri(self.publicKey().current().toHex(), inviteCode));

		String title = ("Send invite to ") + receiver;
		Intent chooser = Intent.createChooser(sharingIntent, title);

		if (sharingIntent.resolveActivity(context.getPackageManager()) != null)
		    context.startActivity(chooser);
	}


	public static String buildSneerUri(String puk, String inviteCode) {
		return "http://sneer.me/public-key?" + puk + "&invite=" + inviteCode;
	}

}
