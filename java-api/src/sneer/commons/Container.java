package sneer.commons;

import java.lang.Object;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static sneer.commons.exceptions.Exceptions.check;


public class Container {

	static private final WeakHashMap<Object, Object> containersByComponent = new WeakHashMap<Object, Object>();
	private static final Object MULTIPLE = "COMPONENT IN MULTIPLE CONTAINERS";

	/** Used by components to load others: Container.of(this).produce(OtherComponent.class) */
	synchronized
	static public Container of(Object component) {
		Object ret = containersByComponent.get(component);
		if (ret == null)     throw new IllegalArgumentException("Unable to find container for component: " + component);
		if (ret == MULTIPLE) throw new IllegalArgumentException("Component '" + component + "' is in more than one container.");
		return (Container)ret;
	}


	private final ComponentLoader loader;
	private final Map<Object, Object> componentsByHandle = new HashMap<Object, Object>();

	public Container(ComponentLoader loader) {
		this.loader = loader;
	}

	/** @param handle Typically an interface but can be anything the component loader understands. */
	public <T> T produce(Object handle) {
		synchronized (Container.class) {
			T cached = (T) componentsByHandle.get(handle);
			if (cached != null) return cached;

			T loaded = loader.load(handle, this);
			keep(handle, loaded);
			return loaded;
		}
	}


	public void inject(Object handle, Object component) {
		synchronized (Container.class) {
			check(!componentsByHandle.containsKey(handle));

			keep(handle, component);
		}
	}

	private void keep(Object handle, Object component) {
		if (containersByComponent.containsKey(component))
			containersByComponent.put(component, MULTIPLE);
		else
			containersByComponent.put(component, this);

		componentsByHandle.put(handle, component);
	}

	public interface ComponentLoader {
		<T> T load(Object handle, Container container);
	}

}
