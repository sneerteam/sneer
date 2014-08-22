package sneer.android.main.ui.utils;

import sneer.*;
import android.app.*;
import android.content.*;

public class Puk {

	public static void sendYourPublicKey(Activity activity, Party party, boolean self) {
		sendYourPublicKey(activity, party, self, null);
	}
	
	public static void sendYourPublicKey(Activity activity, Party party, boolean self, String nickname) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sneer public key");
		sharingIntent.putExtra(Intent.EXTRA_TEXT, buildSneerUri(party.publicKey().current().asBitcoinAddress()));
	
		String title;
		if(self)
			title = "Share Your Public Key";
		else
			title = "Share " + nickname  + "'s Public Key";
		Intent chooser = Intent.createChooser(sharingIntent, title);
		
		if (sharingIntent.resolveActivity(activity.getPackageManager()) != null) {
		    activity.startActivity(chooser);
		}
		
	}

	
	public static String buildSneerUri(String puk) {
		
		return "http://sneer.me/public-key?" + puk;
	}

}
