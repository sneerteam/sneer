package sneer.admin;

public class SneerAdminFactory {

	public static SneerAdmin create(Object db) {
		ServiceProvider provider = serviceProvider();
		return provider == null ? null : provider.create(db);
	}

	public interface ServiceProvider {
		SneerAdmin create(Object db);
	}

	private static ServiceProvider serviceProvider() {
		try {
			return ((ServiceProvider)Class.forName("sneer.main.SneerAdminFactoryServiceProvider").newInstance());
		} catch (Exception e) {
			return null;
		}
	}
}
