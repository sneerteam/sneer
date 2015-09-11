package sneer.android.ui;

import android.content.Context;
import android.content.Intent;

import sneer.convos.Convo;
import sneer.convos.Convos;

import static sneer.android.SneerAndroidContainer.component;

public class InviteSender {

	private static final String INVITE_URL = "http://sneer.me/invite?";

	public static void send(Context context, long convoId) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Sneer Invite");
		Convos convos = component(Convos.class);
		Convo convo = convos.getById(convoId).toBlocking().first();
		if (convo.inviteCodePending == null) return;

		sharingIntent.putExtra(Intent.EXTRA_TEXT,
				"Click to add me as a Sneer contact: " + INVITE_URL + convo.inviteCodePending);

		String title = ("Send invite to ") + convo.nickname;
		Intent chooser = Intent.createChooser(sharingIntent, title);

		if (sharingIntent.resolveActivity(context.getPackageManager()) != null)
			context.startActivity(chooser);
	}

}
