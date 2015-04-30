package sneer.android.flux2;

import java.util.HashMap;
import java.util.Map;

import sneer.commons.exceptions.NotImplementedYet;

public interface Dispatcher {

	public <T> T produce(Class<? extends T> component);
	public void dispatch(Object action);


	class Factory {

		private static Map<Object, Dispatcher> instancesByContext = new HashMap<>();

		synchronized
		public static Dispatcher produceFor(Object context) {
			Dispatcher cached = instancesByContext.get(context);
			if (cached != null) return cached;

			Dispatcher created = new Impl();
			instancesByContext.put(context, created);
			return created;
		}

		synchronized
		public static void injectFor(Object context, Dispatcher dispatcher) {
			Dispatcher cached = instancesByContext.get(context);
			if (cached != null) throw new IllegalArgumentException();

			instancesByContext.put(context, dispatcher);
		}
	}


	class Impl implements Dispatcher {

		@Override
		public <T> T produce(Class<? extends T> component) {
			throw new NotImplementedYet(); //TODO instantiate ComponentImpl or, if Impl not found, ComponentSim.
		}

		@Override
		public void dispatch(Object action) {
			throw new NotImplementedYet();
		}
	}
}
