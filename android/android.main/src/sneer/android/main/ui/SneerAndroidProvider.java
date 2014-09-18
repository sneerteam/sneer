package sneer.android.main.ui;

import sneer.Sneer;
import sneer.admin.SneerAdmin;
import sneer.android.main.SneerAndroid;

public class SneerAndroidProvider {
	
	private static SneerAndroid sneerAndroid;

	public static void setInstance(SneerAndroid sneerAndroid) {
		SneerAndroidProvider.sneerAndroid = sneerAndroid;
	}
	
	public static SneerAndroid sneerAndroid() {
		return sneerAndroid;
	}
	
	public static Sneer sneer() {
		return admin().sneer();
	}

	public static SneerAdmin admin() {
		return sneerAndroid().admin();
	}

}
