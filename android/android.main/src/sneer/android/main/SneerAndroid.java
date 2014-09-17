package sneer.android.main;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import android.app.Activity;

public interface SneerAndroid {

	SneerAdmin admin();

	boolean checkOnCreate(Activity activity);

	Sneer sneer();

	boolean isClickable(Message message);

	void doOnClick(Message message);

}