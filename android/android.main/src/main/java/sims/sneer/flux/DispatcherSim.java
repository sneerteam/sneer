package sims.sneer.flux;

import java.util.Arrays;

import rx.Observable;
import sneer.flux.Action;
import sneer.flux.Dispatcher;
import sneer.flux.Request;

public class DispatcherSim implements Dispatcher {

	@Override
	public void dispatch(Action action) {
		System.out.println("Dispatching " + action.type + ": " + Arrays.toString(action.keyValuePairs));
	}

	@Override
	public <T> Observable<T> request(Request<T> request) {
		throw new UnsupportedOperationException("Should not be used by the UI. This method will disappear when we have full-fledged flux.");
	}

}