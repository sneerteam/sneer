package sneer.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;

import static sneer.android.impl.IPCProtocol.SEND_MESSAGE;

public class SneerInstallation {

	public static boolean checkConversationContext(Activity activity) {
		if (wasCalledFromConversation(activity))
			return true;
		promptNeedForSneerConversation(activity);
		return false;
	}


	private static void promptNeedForSneerConversation(Activity activity) {
		Intent sneer = activity.getPackageManager().getLaunchIntentForPackage("sneer.main");
		if (sneer == null)
			start(market(), activity, "To use this app you need to install the Sneer chat app.", "Install Sneer");
		else
			start(sneer   , activity, "This app must be used inside a Sneer conversation.",      "Open Sneer");
	}


	private static Intent market() {
		Intent ret = new Intent(Intent.ACTION_VIEW);
		ret.setData(Uri.parse("market://details?id=sneer.main"));
		return ret;
	}


	private static boolean wasCalledFromConversation(Activity activity) {
		return activity.getIntent().getParcelableExtra(SEND_MESSAGE) != null;
	}


	private static void start(final Intent intent, final Activity activity, String message, String buttonText) {
		AlertDialog alert = new AlertDialog.Builder(activity)
			.setMessage(message)
			.setPositiveButton(buttonText, new OnClickListener() { @Override public void onClick(DialogInterface arg0, int option) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivity(intent);
				activity.finish();
			}})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.create();
		alert.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) {
			activity.finish();
		}});
		alert.show();
	}

}