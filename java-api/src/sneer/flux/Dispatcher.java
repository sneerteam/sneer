package sneer.flux;

import rx.Observable;

public interface Dispatcher {

	void dispatch(Action action);
	void dispatchMap(Object actionMap);

	<T> Observable<T> request(Request<T> request);

}
