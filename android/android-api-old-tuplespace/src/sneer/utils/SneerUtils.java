package sneer.utils;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.*;
import android.net.*;

public class SneerUtils {

    public static void showSneerInstallationMessageIfNecessary(final Activity activity) {
        PackageManager pm = activity.getPackageManager();
        try {
           pm.getPackageInfo("sneer.android.main", PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            showInstallSneerDialog(activity);
        }
    }

	public static void showInstallSneerDialog(final Activity activity) {
		new AlertDialog.Builder(activity)
		.setTitle("You Need the Sneer App")
		.setMessage("Do you want to install it now?")
		.setPositiveButton("Yes", new OnClickListener() {@Override public void onClick(DialogInterface arg0, int option) {
		    Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=sneer.android.main"));
		    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    activity.startActivity(goToMarket);
		    activity.finish();
		}})
		.setNegativeButton("No", new OnClickListener() {@Override public void onClick(DialogInterface arg0, int option) {
		    activity.finish();
		}})
		.setIcon(android.R.drawable.ic_dialog_alert)
		.show();
	}

}
