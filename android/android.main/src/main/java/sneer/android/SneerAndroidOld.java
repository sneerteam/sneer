package sneer.android;

import android.app.Activity;

import java.util.List;

import sneer.Conversation;
import sneer.ConversationItem;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.ipc.Plugin;

public interface SneerAndroidOld {

	SneerAdmin admin();
	Sneer sneer();

	boolean checkOnCreate(Activity activity);

	List<Plugin> plugins();
	void startActivity(Plugin plugin, Conversation conversation);

	boolean isClickable(ConversationItem item);
	void doOnClick(ConversationItem item, Conversation convo);

}
