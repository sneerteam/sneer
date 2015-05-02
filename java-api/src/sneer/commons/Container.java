package sneer.commons;

import java.lang.Class;import java.lang.ClassNotFoundException;import java.lang.Exception;import java.lang.Object;import java.lang.RuntimeException;import java.lang.String;import java.util.HashMap;
import java.util.Map;


public class Container {

	private static Container singletons = new Container();

	synchronized
	static public <T> T singleton(Class<T> intrface) {
		return singletons.component(intrface);
	}

	private Map<Class<?>, Object> instancesByInterface = new HashMap<Class<?>, Object>();

	synchronized
	public <T> T component(Class<T> intrface) { //Method named to use with static import, for example: Foo foo = component(Foo.class)
		T cached = (T)instancesByInterface.get(intrface);
		if (cached != null) return cached;

		T created = instantiate(intrface);
		instancesByInterface.put(intrface, created);
		return created;
	}

	private static <T> T instantiate(Class<T> component) {
		Class<T>           clazz = tryToLoad(component, "Impl");
		if (clazz == null) clazz = tryToLoad(component, "$Sim");
		if (clazz == null) clazz = tryToLoad(component, "Sim");

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to load component " + component + " class: " + clazz, e);
		}

	}

	private static <T> Class<T> tryToLoad(Class<T> component, String suffix) {
		try {
			return (Class<T>) Class.forName(component.getName() + suffix);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}
