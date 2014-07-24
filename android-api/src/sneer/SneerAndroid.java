package sneer;

import sneer.snapi.*;
import android.app.*;
import android.content.*;

public class SneerAndroid {

	public static void startInteractionList(Activity activity, String title, String type, String newInteractionAction) {
		try {
			Intent intent = new Intent("sneer.android.main.INTERACTION_LIST");
			intent.putExtra("title", title);
			intent.putExtra("type", type);
			intent.putExtra("newInteractionAction", newInteractionAction);
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			SneerUtils.showInstallSneerDialog(activity);
		}
	}
	
	public static <T> Session<T> sessionOnAndroidMainThread(Activity activity) {
		return new SneerAndroid(activity).getSession();
	}
	
	private Context context;
	
	public SneerAndroid(Context context) {
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Session<T> getSession() {
		if (!(context instanceof Activity)) {
			throw new IllegalStateException("Context expected to be an Activity, found " + context.getClass().getName());
		}
		return (Session<T>) ((Activity)context).getIntent().getExtras().get("session");
	}


}
