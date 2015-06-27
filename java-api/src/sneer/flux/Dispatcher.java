package sneer.flux;

import rx.Observable;

public interface Dispatcher {

	void dispatch(Action action);

	<T> Observable<T> request(Request<T> request);

}
