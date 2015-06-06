package sneer.commons;

import java.lang.Class;
import java.lang.Object;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static sneer.commons.exceptions.Exceptions.check;


public class Container {

	static private final WeakHashMap<Object, Container> containersByComponent = new WeakHashMap<Object, Container>();

	/** Used by components to load others: Container.of(this).produce(OtherComponent.class) */
	synchronized
	static public Container of(Object component) {
		Container ret = containersByComponent.get(component);
		if (ret == null) throw new IllegalArgumentException("Unable to find single container of " + component + " (it is contained by none or more than one container)." );

		return ret;
	}


	private final ComponentLoader loader;
	private final Map<Class<?>, Object> componentsByInterface = new HashMap<Class<?>, Object>();

	public Container(ComponentLoader loader) {
		this.loader = loader;
	}

	public <T> T produce(Class<T> componentInterface) {
		synchronized (Container.class) {
			T cached = (T) componentsByInterface.get(componentInterface);
			if (cached != null) return cached;

			T loaded = loader.load(componentInterface, this);
			keep(componentInterface, loaded);
			return loaded;
		}
	}


	public void inject(Class<?> componentInterface, Object component) {
		synchronized (Container.class) {
			check(componentInterface.isAssignableFrom(component.getClass()));
			check(!componentsByInterface.containsKey(componentInterface));

			keep(componentInterface, component);
		}
	}

	private void keep(Class<?> componentInterface, Object component) {
		componentsByInterface.put(componentInterface, component);
		if (containersByComponent.containsKey(component))
			containersByComponent.remove(component); //Component is in more than one container. See Container.of(component) method.
		else
			containersByComponent.put(component, this);
	}


	public interface ComponentLoader {
		<T> T load(Class<T> componentInterface, Container container);
	}

}
