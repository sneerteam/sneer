package sneer.android.ui;

import android.content.Context;
import android.content.Intent;

import sneer.Party;
import sneer.Sneer;
import sneer.android.SneerAndroidContainer;
import sneer.android.SneerAndroidSingleton;
import sneer.commons.Container;
import sneer.convos.Convo;
import sneer.convos.Convos;

import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.SneerAndroidSingleton.sneer;

public class InviteSender {

	public static void send(Context context, long convoId) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Sneer Invite");
        Convos convos = component(Convos.class);
        Convo convo = convos.getById(convoId).toBlocking().first();
        if (convo.inviteCodePending == null) return;

        sharingIntent.putExtra(Intent.EXTRA_TEXT,
				"If you don't have the Sneer app, install it using the Play Store: https://play.google.com/store/apps/details?id=sneer.main\n\n" +
				"Then, tap to add me as a Sneer contact: " +
				buildSneerUri(convos.ownPuk(), convo.inviteCodePending));

		String title = ("Send invite to ") + convo.nickname;
		Intent chooser = Intent.createChooser(sharingIntent, title);

		if (sharingIntent.resolveActivity(context.getPackageManager()) != null)
		    context.startActivity(chooser);
	}


	public static String buildSneerUri(String puk, String inviteCode) {
		return "http://sneer.me/public-key?" + puk + "&invite=" + inviteCode;
	}

}
