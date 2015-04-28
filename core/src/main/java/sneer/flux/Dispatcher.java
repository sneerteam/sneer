package sneer.flux;

/**
 * Intermediates the creation of clojure components defined via `defcomponent`
 * and the dispatch of actions.
 */
public class Dispatcher {

	public static <T> T createInstance(Class<T> component, Object... args) {
		try {
			return ((T) Class.forName(component.getName() + "ServiceProvider")
									 .getDeclaredConstructors()[0]
									 .newInstance(args));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
