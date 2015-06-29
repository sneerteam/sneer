package sneer.android;

import sneer.flux.Action;
import sneer.flux.Dispatcher;

import static sneer.android.SneerAndroidContainer.component;

public class SneerAndroidFlux {

	public static void dispatch(Action action) {
		component(Dispatcher.class).dispatch(action);
	}

}
