package sneer.android;

import android.app.Activity;
import android.content.Context;

import java.util.List;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.ipc.Plugin;

public interface SneerAndroid {

	SneerAdmin admin();
	Sneer sneer();

	boolean checkOnCreate(Activity activity);

	List<Plugin> plugins();

	boolean isClickable(Message message);
	void doOnClick(Message message);

}
