package sneer.rx;

import rx.*;

/** An Observable that has a notion of a current value. */
public interface Observed<T> {
	
	/** @return The most recent item emitted by observable(). */
	public T current();
	
	/** @return An Observable that will emit the current() and future values. */
	public Observable<T> observable();

}
