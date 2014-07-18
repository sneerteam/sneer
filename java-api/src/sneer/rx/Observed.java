package sneer.rx;

import rx.*;

/** An Observable for which we know the most recent value. */
public interface Observed<T> {
	
	/** @return The most recent item emitted by observable(). */
	public T mostRecent();
	
	/** @return An Observable that will emit the mostRecent() and future values. */
	public Observable<T> observable();

}
