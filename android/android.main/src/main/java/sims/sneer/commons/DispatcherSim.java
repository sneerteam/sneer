package sims.sneer.commons;

import java.util.Arrays;

import rx.Observable;
import sneer.flux.Action;
import sneer.flux.ActionBase;
import sneer.flux.Dispatcher;
import sneer.flux.Request;

public class DispatcherSim implements Dispatcher {

	@Override
	public void dispatch(Action action) {
		print(action);
	}

	@Override @SuppressWarnings("unchecked")
	public <T> Observable<T> request(Request<T> request) {
		print(request);
		Object ret = null;
		if (request.type.equals("request-invite")) ret = 1042;
//		if (request.type.equals("some-other-request")) ret = "some-other-response";
		return Observable.just((T)ret);
	}

	static private void print(ActionBase action) {
		System.out.println("Dispatching " + action.type + ": " + Arrays.toString(action.keyValuePairs));
	}

}