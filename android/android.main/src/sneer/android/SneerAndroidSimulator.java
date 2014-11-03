package sneer.android;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.impl.simulator.SneerAdminSimulator;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

public final class SneerAndroidSimulator implements SneerAndroid {
	private SneerAdmin admin;
	private Context context;

	public SneerAndroidSimulator(Context context) {
		this.context = context;
		SneerAdminSimulator ret = new SneerAdminSimulator();
		Sneer sneer = ret.sneer();
		sneer.profileFor(sneer.self()).setOwnName("Neide da Silva"); // Comment this line to get an empty name.
		admin = ret;
	}

	@Override
	public Sneer sneer() {
		return admin.sneer();
	}

	@Override
	public boolean isClickable(Message message) {
		return true;
	}

	@Override
	public void doOnClick(Message message) {
		Toast.makeText(context, "Message clicked: " + message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean checkOnCreate(Activity activity) {
		return true;
	}

	@Override
	public SneerAdmin admin() {
		return admin;
	}

}
