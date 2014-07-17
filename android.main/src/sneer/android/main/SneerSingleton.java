package sneer.android.main;


public class SneerSingleton {
	
	public static final SneerAdmin SNEER_ADMIN =
		new SneerAdminSimulator();
		//new SneerAdminImpl();

}
