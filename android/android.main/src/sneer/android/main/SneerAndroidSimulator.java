package sneer.android.main;

import sneer.Message;
import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.impl.simulator.SneerAdminSimulator;
import android.app.Activity;
import android.content.Context;

public final class SneerAndroidSimulator implements SneerAndroid {
	private SneerAdmin admin;

	public SneerAndroidSimulator() {
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
		return false;
	}

	@Override
	public void doOnClick(Message message) {
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
