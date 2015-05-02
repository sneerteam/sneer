package sneer.commons;

import java.lang.Class;import java.lang.ClassNotFoundException;import java.lang.Exception;import java.lang.Object;import java.lang.RuntimeException;import java.lang.String;import java.util.HashMap;
import java.util.Map;


public class Container {

	private final ComponentLoader loader;
	private final Map<Class<?>, Object> instancesByInterface = new HashMap<Class<?>, Object>();

	public Container(ComponentLoader loader) {
		this.loader = loader;
	}

	synchronized
	public <T> T produce(Class<T> componentInterface) {
		T cached = (T)instancesByInterface.get(componentInterface);
		if (cached != null) return cached;

		T loaded = loader.load(componentInterface);
		instancesByInterface.put(componentInterface, loaded);
		return loaded;
	}


	interface ComponentLoader {
		<T> T load(Class<T> componentInterface);
	}


	public static ComponentLoader withPadding(final String prefix, final String suffix) {
		return new ComponentLoader() { @Override public <T> T load(Class<T> intrface) {
			try {
				return (T)Class.forName(prefix + intrface.getName() + suffix).newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}};
	}

}
