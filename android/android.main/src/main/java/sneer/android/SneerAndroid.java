package sneer.android;

import android.app.Activity;

import java.util.List;

import sneer.ConversationItem;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.ipc.Plugin;
import sneer.convos.Convo;

public interface SneerAndroid {

	SneerAdmin admin();
	Sneer sneer();

	boolean checkOnCreate(Activity activity);

	List<Plugin> plugins();

	boolean isClickable(ConversationItem item);
	void doOnClick(ConversationItem item, Convo convo);

}
