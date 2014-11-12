package sneer.android.utils;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import sneer.android.impl.SneerAndroidImpl;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class AndroidUtils {

	public static void finishWith(String message, final Activity activity) {
		if (SneerAndroidImpl.errorDialog != null) {
			activity.finish();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(message).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int id) {
			SneerAndroidImpl.errorDialog.dismiss();
			SneerAndroidImpl.errorDialog = null;
			activity.finish();
		}});
		SneerAndroidImpl.errorDialog = builder.create();
		SneerAndroidImpl.errorDialog.show();
	}

	private static void toast(Context context, final String message, final int length) {
		Toast.makeText(context, message, length).show();
	}

	public static void toastOnMainThread(final Context context, final String message, final int length) {
		AndroidSchedulers.mainThread().createWorker().schedule(new Action0() { @Override public void call() {
			toast(context, message, length);
		}});
	}

}
