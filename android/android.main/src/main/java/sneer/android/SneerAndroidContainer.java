package sneer.android;

import sneer.commons.Container;

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
		ComponentLoader     loader = coreLoader();
		if (loader == null) loader = simsLoader();
		return new Container(loader);
	}

	private static ComponentLoader coreLoader() {
		try { return (ComponentLoader)Class.forName("sneer.impl.CoreLoader").newInstance(); }
		catch (ClassNotFoundException e) { return null; }
		catch (Exception e)              { throw new RuntimeException(e); }
	}

	private static ComponentLoader simsLoader() { return new ComponentLoader() { @Override public <T> T load(Class<T> intrface, Container ignored) {
		try {
			return (T) Class.forName("sims." + intrface.getName() + "Sim").newInstance();   //Example: sims.sneer.ConversationListSim
		} catch (Exception e) { throw new RuntimeException(e); }
	}};}

}
