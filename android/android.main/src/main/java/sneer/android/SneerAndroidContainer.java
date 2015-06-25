package sneer.android;

import sneer.commons.Container;
import sneer.commons.Startup;

import static sneer.commons.Container.ComponentLoader;

public class SneerAndroidContainer {

	private static final Container container = initContainer();

	public static <T> T component(Class<T> componentInterface) {
		return container.produce(componentInterface);
	}

	public static Container container() {
		return container;
	}

	private static Container initContainer() {
		Container ret = new Container(loader());
		ret.produce(Startup.class);
		return ret;
	}

	private static ComponentLoader loader() {
		ComponentLoader coreLoader = coreLoader();
		return coreLoader != null
			? coreLoader
			: simsLoader();
	}

	private static ComponentLoader coreLoader() {
		try { return (ComponentLoader)Class.forName("sneer.impl.CoreLoader").newInstance(); }
		catch (ClassNotFoundException e) { return null; }
		catch (Exception e)              { throw new RuntimeException(e); }
	}

	@SuppressWarnings("unchecked")
	private static ComponentLoader simsLoader() { return new ComponentLoader() { @Override public <T> T load(Object handle, Container ignored) {
		try {
			return (T) Class.forName("sims." + ((Class<T>)handle).getName() + "Sim").newInstance();   //Example: sims.sneer.ConversationListSim
		} catch (Exception e) { throw new RuntimeException(e); }
	}};}

}
