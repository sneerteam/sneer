package sneer.android.main;

import java.util.*;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.view.*;
import android.widget.*;

public class ImageUtils {

	/** Detect the available intent and open a new dialog. */
	public static void openMediaSelector(Activity context) {
		Intent camIntent = new Intent("android.media.action.IMAGE_CAPTURE");
		Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
		gallIntent.setType("image/*");

		// look for available intents
		List<ResolveInfo> info = new ArrayList<ResolveInfo>();
		List<Intent> yourIntentsList = new ArrayList<Intent>();
		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> listCam = packageManager.queryIntentActivities(camIntent, 0);
		for (ResolveInfo res : listCam) {
			final Intent finalIntent = new Intent(camIntent);
			finalIntent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
			yourIntentsList.add(finalIntent);
			info.add(res);
		}
		List<ResolveInfo> listGall = packageManager.queryIntentActivities(
				gallIntent, 0);
		for (ResolveInfo res : listGall) {
			final Intent finalIntent = new Intent(gallIntent);
			finalIntent.setComponent(new ComponentName(
					res.activityInfo.packageName, res.activityInfo.name));
			yourIntentsList.add(finalIntent);
			info.add(res);
		}

		// show available intents
		openDialog(context, yourIntentsList, info);
	}

	/** Open a new dialog with the detected items. */
	private static void openDialog(final Activity context,
			final List<Intent> intents, List<ResolveInfo> activitiesInfo) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(context.getResources().getString(
				R.string.action_select));
		dialog.setAdapter(buildAdapter(context, activitiesInfo),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						Intent intent = intents.get(id);
						context.startActivityForResult(intent, 1);
					}
				});

		dialog.setNeutralButton(
				context.getResources().getString(R.string.action_cancel),
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		dialog.show();
	}

	/** Build the list of items to show using the intent_listview_row layout. */
	private static ArrayAdapter<ResolveInfo> buildAdapter(final Context context, final List<ResolveInfo> activitiesInfo) {
		return new ArrayAdapter<ResolveInfo>(context, R.layout.list_item_action_pick_image, R.id.actionList, activitiesInfo) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				ResolveInfo res = activitiesInfo.get(position);
				ImageView image = (ImageView) view.findViewById(R.id.icon);
				image.setImageDrawable(res.loadIcon(context.getPackageManager()));
				TextView textview = (TextView) view.findViewById(R.id.actionList);
				textview.setText(res.loadLabel(context.getPackageManager()).toString());
				return view;
			}
		};
	}
}
