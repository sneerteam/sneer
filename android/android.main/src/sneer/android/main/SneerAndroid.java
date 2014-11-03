package sneer.android.main;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import android.app.Activity;

public interface SneerAndroid {

	SneerAdmin admin();
	Sneer sneer();

	boolean checkOnCreate(Activity activity);

	boolean isClickable(Message message);
	void doOnClick(Message message);

}