package sneer.android;

import android.app.Activity;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;

public interface SneerAndroid {

	SneerAdmin admin();
	Sneer sneer();

	boolean checkOnCreate(Activity activity);

	boolean isClickable(Message message);
	void doOnClick(Message message);

}
