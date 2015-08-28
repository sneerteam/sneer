package sneer.android;

import rx.Observable;
import sneer.flux.Action;
import sneer.flux.Dispatcher;
import sneer.flux.Request;

import static sneer.android.SneerAndroidContainer.component;

public class SneerAndroidFlux {

	public static void dispatch(Action action) {
		component(Dispatcher.class).dispatch(action);
	}

	public static <T> Observable<T> request(Request<T> request) {
		return component(Dispatcher.class).request(request);
	}

}
