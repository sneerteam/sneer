package sneer.android.utils;

import android.app.Activity;
import android.content.Intent;
import sneer.Party;

public class Puk {

	public static void sendYourPublicKey(Activity activity, Party party, boolean self, String nickname) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, (self ? "My" : nickname + "'s") + " Sneer public key");
		sharingIntent.putExtra(Intent.EXTRA_TEXT, buildSneerUri(party.publicKey().current().toHex()));

		String title = self
				? "Share Your Public Key"
				: "Share " + nickname  + "'s Public Key";
		Intent chooser = Intent.createChooser(sharingIntent, title);

		if (sharingIntent.resolveActivity(activity.getPackageManager()) != null) {
		    activity.startActivity(chooser);
		}
	}


	public static String buildSneerUri(String puk) {
		return "http://sneer.me/public-key?" + puk;
	}

}
